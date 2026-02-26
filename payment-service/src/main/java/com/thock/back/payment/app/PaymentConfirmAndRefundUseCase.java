package com.thock.back.payment.app;


import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.payment.domain.*;
import com.thock.back.payment.domain.dto.request.PaymentConfirmRequestDto;
import com.thock.back.payment.domain.dto.response.TossErrorResponseDto;
import com.thock.back.payment.out.PaymentMemberRepository;
import com.thock.back.payment.out.PaymentRepository;
import com.thock.back.payment.out.WalletRepository;
import com.thock.back.shared.payment.dto.PaymentCancelRequestDto;
import com.thock.back.shared.payment.dto.PaymentDto;
import com.thock.back.shared.payment.dto.RefundResponseDto;
import com.thock.back.shared.payment.event.PaymentCompletedEvent;
import com.thock.back.shared.payment.event.PaymentRefundCompletedEvent;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConfirmAndRefundUseCase {
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final PaymentMemberRepository paymentMemberRepository;
    private final EventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String CANCEL_PATH = "/v1/payments/{paymentKey}/cancel";

    @Value("${custom.payment.toss.baseUrl:https://api.tosspayments.com}")
    private String tossBaseUrl;

    @Value("${custom.payment.toss.payments.secretKey}")
    private String tossSecretKey;

    // TODO: Payment가 Request상태일 때 TTL을 걸어서 일정 시간이 지나면 삭제 되게 해야할듯?

    /**
     * 토스페이먼츠 검증 기능
     **/
    public Map<String, Object> confirmPayment(PaymentConfirmRequestDto req) {
        log.info("토스 결제 신청 - orderId={}, Amount={}", req.getOrderId(), req.getAmount());
        Payment payment = paymentRepository.findByOrderId(req.getOrderId())
                .orElseThrow(() -> {
                    log.error("결제 조회 실패 - orderId={}", req.getOrderId());
                    return new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
                });
        PaymentMember member = paymentMemberRepository.findById(payment.getBuyer().getId())
                .orElseThrow(() -> {
                    log.error("멤버 조회 실패 - orderId={}, memberId={}", req.getOrderId(), payment.getBuyer().getId());
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
        Wallet wallet = walletRepository.findByHolderId(member.getId())
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - orderId={}, memberId={}", req.getOrderId(), member.getId());
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });
        // 상태 체크
        if (payment.getStatus() != PaymentStatus.REQUESTED) {
            log.error("결제 상태가 요청이 아닙니다. - orderId={}", req.getOrderId());
            throw new CustomException(ErrorCode.PAYMENT_NOT_REQUEST);
        }
        // 이전 상태 저장
        PaymentStatus previousStatus = payment.getStatus();

        // 상태를 PG_PENDING 변경 (별도 트랜잭션으로 커밋)
        transactionTemplate.executeWithoutResult(status -> {
            Payment p = paymentRepository.findById(payment.getId()).orElseThrow();
            p.updatePaymentStatus(PaymentStatus.PG_PENDING);
            paymentRepository.save(p);
        });
        // 바디 채우기
        Map<String, Object> body = Map.of(
                "paymentKey", req.getPaymentKey(),
                "orderId", req.getOrderId(),
                "amount", req.getAmount()
        );

        // 외부 API 통신
        Map<String, Object> confirmResponse;

        try {
            confirmResponse = tossApi(body, req.getOrderId(), CONFIRM_PATH);
        } catch (Exception e) {
            // 실패 시 상태 복원 (별도 트랜잭션으로 커밋)
            transactionTemplate.executeWithoutResult(status -> {
                Payment p = paymentRepository.findById(payment.getId()).orElseThrow();
                p.updatePaymentStatus(previousStatus);
                paymentRepository.save(p);
            });
            log.error("토스 API 호출 실패로 상태 복원 - orderId={}, status={}", req.getOrderId(), previousStatus);
            throw e;
        }

        // confirm 결과 검증 (토스 응답)
        Integer approvedAmount = (Integer) confirmResponse.get("totalAmount");
        if (!payment.getPgAmount().equals(approvedAmount.longValue())) {
            log.error("토스 결제 금액 불일치 - orderId={}, expected={}, actual={}", req.getOrderId(), payment.getPgAmount(), approvedAmount);
            transactionTemplate.executeWithoutResult(status -> {
                Payment p = paymentRepository.findById(payment.getId()).orElseThrow();
                p.updatePaymentStatus(previousStatus);
                paymentRepository.save(p);
            });
            throw new CustomException(ErrorCode.TOSS_AMOUNT_NOT_MATCH);
        }
        // 토스 Confirm 이후 내부 서버 시스템 처리
        // 별도 트랜잭션으로 분리 및 보상 작업 수행
        try {
            transactionTemplate.executeWithoutResult(status -> {
                // 검증 후 process
                payment.updatePaymentStatus(PaymentStatus.PG_PAID);
                payment.updatePaymentKey(req.getPaymentKey());
                paymentRepository.save(payment);
                payment.createPaymentLogEvent();
                log.info("토스페이먼츠 결제 완료 - orderId={}, amount={} ", req.getOrderId(), approvedAmount);
                PaymentDto paymentDto = new PaymentDto(payment.getId(),
                        payment.getOrderId(),
                        payment.getPaymentKey(),
                        payment.getBuyer().getId(),
                        payment.getPgAmount(),
                        payment.getAmount(),
                        payment.getCreatedAt(),
                        payment.getRefundedAmount()
                );

                // 지갑 입금
                wallet.depositBalance(payment.getAmount());
                walletRepository.save(wallet);
                wallet.createBalanceLogEvent(payment.getAmount(), EventType.주문_입금);

                // 지갑 출금
                wallet.withdrawBalance(payment.getAmount());
                walletRepository.save(wallet);
                wallet.createBalanceLogEvent(payment.getAmount(), EventType.주문_출금);

                // 결제 상태 변경
                payment.updatePaymentStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(payment);

                // 결제 완료 이벤트 발행
                eventPublisher.publish(
                        new PaymentCompletedEvent(
                                paymentDto
                        )
                );
            });
        } catch (Exception e) {
            // 후속 처리 실패 시 상태 복원 (별도 트랜잭션으로 커밋)
            transactionTemplate.executeWithoutResult(status -> {
                Payment p = paymentRepository.findById(payment.getId()).orElseThrow();
                p.updatePaymentStatus(PaymentStatus.PG_PAID_BUT_FAILED); // PG_PAID_BUT_FAILED로 변경
                paymentRepository.save(p);
            });
            log.error("결제 실패로 상태 복원 - orderId={}, status={}", req.getOrderId(), previousStatus, e);
            throw e;
        }
        return confirmResponse;
    }

    /**
     * 토스페이먼츠 취소 기능
     **/
    public void cancelToss(PaymentCancelRequestDto req) {
        // 검증 (트랜잭션 외부에서 조회)
        log.info("토스 환불 신청 - orderId={}, refundAmount={}", req.orderId(), req.amount());
        Payment payment = paymentRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> {
                    log.warn("결제 조회 실패 - orderId={}", req.orderId());
                    return new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
                });

        // 상태 체크
        if (payment.getStatus() == PaymentStatus.CANCELED || payment.getStatus() == PaymentStatus.REQUESTED) {
            log.warn("이미 취소된 결제입니다 - orderId={}", req.orderId());
            throw new CustomException(ErrorCode.PAYMENT_NOT_COMPLETE);
        }

        // 환불 이유 체크
        if (req.cancelReason() == null) {
            log.warn("환불 사유가 없습니다 - orderId={}", req.orderId());
            throw new CustomException(ErrorCode.REFUND_NOT_CANCEL_REASON);
        }

        // 1. 0원 / 음수 방지
        if (req.amount() <= 0) {
            log.warn("환불 금액이 유효하지 않습니다 - orderId={}, amount={}", req.orderId(), req.amount());
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT);
        }

        // 이전 상태 저장
        PaymentStatus previousStatus = payment.getStatus();

        // 상태를 CANCELED_PENDING으로 변경 (별도 트랜잭션으로 커밋)
        transactionTemplate.executeWithoutResult(status -> {
            Payment p = paymentRepository.findById(payment.getId()).orElseThrow();
            p.updatePaymentStatus(PaymentStatus.CANCELED_PENDING);
            paymentRepository.save(p);
        });

        // 바디 채우기
        Map<String, Object> body = Map.of(
                "paymentKey", payment.getPaymentKey(),
                "cancelReason", req.cancelReason(),
                "cancelAmount", req.amount()
        );

        // 외부 api 통신
        Map<String, Object> confirmResponse;
        try {
            confirmResponse = tossApi(body, req.orderId(), CANCEL_PATH, payment.getPaymentKey());
        } catch (Exception e) {
            // 실패 시 상태 복원 (별도 트랜잭션으로 커밋)
            transactionTemplate.executeWithoutResult(status -> {
                Payment p = paymentRepository.findById(payment.getId()).orElseThrow();
                p.updatePaymentStatus(previousStatus);
                paymentRepository.save(p);
            });
            log.warn("토스 API 호출 실패로 상태 복원 - orderId={}, status={}", req.orderId(), previousStatus);
            throw e;
        }

        // cancel 결과 검증 (토스 응답)
        String tossStatus = (String) confirmResponse.get("status");
        List<Map<String, Object>> cancels = (List<Map<String, Object>>) confirmResponse.get("cancels");
        Map<String, Object> lastCancel = cancels.get(cancels.size() - 1);  // 마지막 취소 건
        Number cancelAmountNum = (Number) lastCancel.get("cancelAmount");
        Long amount = cancelAmountNum.longValue();

        if (tossStatus.equals("PARTIAL_CANCELED") || tossStatus.equals("CANCELED")) {
            // 성공 시 결과 처리 (별도 트랜잭션으로 커밋)
            transactionTemplate.executeWithoutResult(status -> {
                Payment p = paymentRepository.findById(payment.getId()).orElseThrow();
                Wallet w = walletRepository.findByHolderId(p.getBuyer().getId()).orElseThrow();

                // 부분 취소 입금
                if (!amount.equals(p.getAmount())) {
                    if (p.updatePaymentRefundedAmount(amount)) {
                        if(p.getRefundedAmount().equals(p.getAmount())){
                            p.updatePaymentStatus(PaymentStatus.CANCELED);
                            log.info("토스 전액 환불 완료 - orderId={}, refundAmount={}", req.orderId(), amount);
                        } else {
                            p.updatePaymentStatus(PaymentStatus.PARTIALLY_CANCELED);
                            log.info("토스 부분 환불 완료 - orderId={}, refundAmount={}", req.orderId(), amount);
                        }
                        w.depositBalance(amount);
                        walletRepository.save(w);
                        paymentRepository.save(p);
                        w.createBalanceLogEvent(req.amount(), EventType.부분취소_입금);
                        // 환불 완료 이벤트 발행
                        eventPublisher.publish(
                                new PaymentRefundCompletedEvent(
                                        new RefundResponseDto(
                                                payment.getBuyer().getId(),
                                                payment.getOrderId(),
                                                amount
                                        )
                                )
                        );
                    }
                } else {
                    // 전액 환불
                    if (p.updatePaymentRefundedAmount(amount)) {
                        p.updatePaymentStatus(PaymentStatus.CANCELED);
                        w.depositBalance(amount);
                        walletRepository.save(w);
                        paymentRepository.save(p);
                        w.createBalanceLogEvent(p.getAmount(), EventType.전체취소_입금);
                        log.info("토스 전액 환불 완료 - orderId={}, refundAmount={}", req.orderId(), amount);
                        // 환불 완료 이벤트 발행
                        eventPublisher.publish(
                                new PaymentRefundCompletedEvent(
                                        new RefundResponseDto(
                                                payment.getBuyer().getId(),
                                                payment.getOrderId(),
                                                amount
                                        )
                                )
                        );
                    }
                }
            });
        }
    }

    /**
     * 내부 결제 취소 기능
     **/
    @Retryable(
            retryFor = OptimisticLockException.class,
            maxAttempts = 3
    )
    public void cancelPayment(PaymentCancelRequestDto req) {
        // 결제 확인
        log.info("내부 결제 환불 신청 - orderId={}", req.orderId());
        Payment payment = paymentRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> {
                    log.warn("결제 조회 실패 - orderId={}", req.orderId());
                    return new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
                });
        // 지갑 확인
        Wallet wallet = walletRepository.findByHolderId(payment.getBuyer().getId())
                .orElseThrow(() -> {
                    log.warn("지갑 조회 실패 - memberId={}", payment.getBuyer().getId());
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });

        // 상태 체크 - 유저가 부분취소를 여러번 할 수 있음
        if (payment.getStatus() == PaymentStatus.CANCELED || payment.getStatus() == PaymentStatus.REQUESTED) {
            log.warn("이미 취소된 결제입니다 - orderId={}", req.orderId());
            throw new CustomException(ErrorCode.PAYMENT_NOT_COMPLETE);
        }

        // 1. 0원 / 음수 방지
        if (req.amount() <= 0) {
            log.warn("환불 금액이 유효하지 않습니다 - orderId={}, amount={}", req.orderId(), req.amount());
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT);
        }
        // 3. 환불
        // 전액 환불
        if (req.amount().equals(payment.getAmount())) {
            if (payment.updatePaymentRefundedAmount(req.amount())) {
                payment.updatePaymentStatus(PaymentStatus.CANCELED);
                paymentRepository.save(payment);
                wallet.depositBalance(req.amount());
                walletRepository.save(wallet);
                wallet.createBalanceLogEvent(req.amount(), EventType.전체취소_입금);
                log.info("내부 결제 전액 환불 완료 - orderId={}, refundAmount={}", req.orderId(), req.amount());
            }
            // 환불 완료 이벤트 발행
            eventPublisher.publish(
                    new PaymentRefundCompletedEvent(
                            new RefundResponseDto(
                                    payment.getBuyer().getId(),
                                    payment.getOrderId(),
                                    req.amount()
                            )
                    )
            );
        }
        // 부분 환불
        else {
            if (payment.updatePaymentRefundedAmount(req.amount())) {
                payment.updatePaymentStatus(PaymentStatus.PARTIALLY_CANCELED);
                paymentRepository.save(payment);
                wallet.depositBalance(req.amount());
                walletRepository.save(wallet);
                wallet.createBalanceLogEvent(req.amount(), EventType.부분취소_입금);
                log.info("내부 결제 부분 환불 완료 - orderId={}, refundAmount={}", req.orderId(), req.amount());
            }
            // 환불 완료 이벤트 발행
            eventPublisher.publish(
                    new PaymentRefundCompletedEvent(
                            new RefundResponseDto(
                                    payment.getBuyer().getId(),
                                    payment.getOrderId(),
                                    req.amount()
                            )
                    )
            );
        }
    }

    public void cancelBeforePayment(String orderId) {
        log.info("결제 전 환불 신청 - orderId={}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("결제 조회 실패 - orderId={}", orderId);
                    return new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
                });
        payment.updatePaymentStatus(PaymentStatus.CANCELED);
        paymentRepository.save(payment);
        payment.createPaymentLogEvent();
        log.info("결제 전 환불 완료- orderId={}, refundedAmount={}", orderId, payment.getAmount());
    }

    // 토스 외부 호출 API
    public Map<String, Object> tossApi(Map<String,Object> body, String orderId, String path, Object... uriVariables) {
        Map<String, Object> confirmResponse;

        confirmResponse = WebClient.builder()
                .baseUrl(tossBaseUrl)
                .defaultHeaders(headers -> {
                    String auth = Base64.getEncoder()
                            .encodeToString((tossSecretKey + ":").getBytes());
                    headers.set("Authorization", "Basic " + auth);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .build()
                .post()
                .uri(path, uriVariables)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(TossErrorResponseDto.class)
                                .flatMap(error -> {
                                    log.error("토스 결제 확인 실패 - orderId={}, error={}", orderId, error.message());
                                    return Mono.error(
                                            new CustomException(
                                                    ErrorCode.TOSS_CONFIRM_FAIL,
                                                    error.message()
                                            )
                                    );
                                }))
                .bodyToMono(Map.class)
                .block();

        return confirmResponse;
    }
}
