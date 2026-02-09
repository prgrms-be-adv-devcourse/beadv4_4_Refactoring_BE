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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConfirmService {
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final PaymentMemberRepository paymentMemberRepository;
    private final EventPublisher eventPublisher;
    private static final String TOSS_BASE_URL = "https://api.tosspayments.com";
    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String CANCEL_PATH = "/v1/payments/{paymentKey}/cancel";

    @Value("${custom.payment.toss.payments.secretKey}")
    private String tossSecretKey;

    /**
     * 토스페이먼츠 검증 기능
     **/
    @Retryable(
            retryFor = OptimisticLockException.class,
            maxAttempts = 3
    )
    @Transactional
    public Map<String, Object> confirmPayment(PaymentConfirmRequestDto req) {
        Payment payment = paymentRepository.findByOrderId(req.getOrderId())
                .orElseThrow(() -> {
                    log.error("결제 조회 실패 - orderId={}", req.getOrderId());
                    return new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
                });
        PaymentMember member = paymentMemberRepository.findById(payment.getBuyer().getId())
                .orElseThrow(() -> {
                    log.error("멤버 조회 실패 - memberId={}", payment.getBuyer().getId());
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });
        Wallet wallet = walletRepository.findByHolderId(member.getId())
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - memberId={}", member.getId());
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });
        // 상태 체크
        if (payment.getStatus() != PaymentStatus.REQUESTED) {
            log.error("결제 상태가 요청이 아닙니다. - orderId={}", req.getOrderId());
            throw new CustomException(ErrorCode.PAYMENT_NOT_REQUEST);
        }

        Map<String, Object> body = Map.of(
                "paymentKey", req.getPaymentKey(),
                "orderId", req.getOrderId(),
                "amount", req.getAmount()
        );

        Map<String, Object> confirmResponse = WebClient.builder()
                .baseUrl(TOSS_BASE_URL)
                .defaultHeaders(headers -> {
                    String auth = Base64.getEncoder()
                            .encodeToString((tossSecretKey + ":").getBytes());
                    headers.set("Authorization", "Basic " + auth);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .build()
                .post()
                .uri(CONFIRM_PATH)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(TossErrorResponseDto.class)
                                .flatMap(error -> {
                                    log.error("토스 결제 확인 실패 - orderId={}, error={}", req.getOrderId(), error.message());
                                    return Mono.error(
                                            new CustomException(
                                                    ErrorCode.TOSS_CONFIRM_FAIL,
                                                    error.message()
                                            )
                                    );
                }))
                .bodyToMono(Map.class)
                .block();

        // confirm 결과 검증 (토스 응답)
        Integer approvedAmount = (Integer) confirmResponse.get("totalAmount");
        if (!payment.getPgAmount().equals(approvedAmount.longValue())) {
            log.error("토스 결제 금액 불일치 - orderId={}, expected={}, actual={}", req.getOrderId(), payment.getPgAmount(), approvedAmount);
            payment.updatePaymentStatus(PaymentStatus.CANCELED);
            throw new CustomException(ErrorCode.TOSS_AMOUNT_NOT_MATCH);
        }

        // 검증 후 process
        payment.updatePaymentStatus(PaymentStatus.COMPLETED);
        payment.updatePaymentKey(req.getPaymentKey());
        paymentRepository.save(payment);
        payment.createPaymentLogEvent();
        log.info("토스페이먼츠 검증 완료 - orderId={}, amount={} ", req.getOrderId(),  approvedAmount);
        PaymentDto paymentDto = new PaymentDto(payment.getId(),
                                                payment.getOrderId(),
                                                payment.getPaymentKey(),
                                                payment.getBuyer().getId(),
                                                payment.getPgAmount(),
                                                payment.getAmount(),
                                                payment.getCreatedAt());
        eventPublisher.publish(
                new PaymentCompletedEvent(
                        paymentDto
                )
        );
        wallet.depositBalance(payment.getAmount());
        walletRepository.save(wallet);
        wallet.createBalanceLogEvent(payment.getAmount(), EventType.주문_입금);
        wallet.withdrawBalance(payment.getAmount());
        walletRepository.save(wallet);
        wallet.createBalanceLogEvent(payment.getAmount(), EventType.주문_출금);
        return confirmResponse;
    }

    /**
     * 토스페이먼츠 취소 기능
     **/
    @Retryable(
            retryFor = OptimisticLockException.class,
            maxAttempts = 3
    )
    @Transactional
    public void cancelToss(PaymentCancelRequestDto req){
        // 검증
        Payment payment = paymentRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> {
                    log.error("결제 조회 실패 - orderId={}", req.orderId());
                    return new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
                });

        // 지갑 확인
        Wallet wallet = walletRepository.findByHolderId(payment.getBuyer().getId())
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - memberId={}", payment.getBuyer().getId());
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });

        // 상태 체크
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            log.error("이미 취소된 결제입니다 - orderId={}", req.orderId());
            throw new CustomException(ErrorCode.PAYMENT_NOT_COMPLETE);
        }

        // 환불 이유 체크
        if (req.cancelReason() == null){
            log.error("환불 사유가 없습니다 - orderId={}", req.orderId());
            throw new CustomException(ErrorCode.REFUND_NOT_CANCEL_REASON);
        }

        // 1. 0원 / 음수 방지
        if (req.amount() <= 0) {
            log.error("환불 금액이 유효하지 않습니다 - orderId={}, amount={}", req.orderId(), req.amount());
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT);
        }

        // 바디 채우기
        Map<String, Object> body = Map.of(
                "paymentKey", payment.getPaymentKey(),
                "cancelReason", req.cancelReason(),
                "cancelAmount", req.amount()
        );

        // WebClient
        Map<String, Object> confirmResponse = WebClient.builder()
                .baseUrl(TOSS_BASE_URL)
                .defaultHeaders(headers -> {
                    String auth = Base64.getEncoder()
                            .encodeToString((tossSecretKey + ":").getBytes());
                    headers.set("Authorization", "Basic " + auth);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .build()
                .post()
                .uri(CANCEL_PATH, payment.getPaymentKey())
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(TossErrorResponseDto.class)
                                .flatMap(error -> {
                                    log.error("토스 결제 취소 실패 - orderId={}, error={}", req.orderId(), error.message());
                                    return Mono.error(
                                            new CustomException(
                                                    ErrorCode.TOSS_CONFIRM_FAIL,
                                                    error.message()
                                            )
                                    );
                }))
                .bodyToMono(Map.class)
                .block();

        // cancel 결과 검증 (토스 응답)
        String status = (String) confirmResponse.get("status");
        List<Map<String, Object>> cancels = (List<Map<String, Object>>) confirmResponse.get("cancels");
        Map<String, Object> lastCancel = cancels.get(cancels.size() - 1);  // 마지막 취소 건
        Number cancelAmountNum = (Number) lastCancel.get("cancelAmount");
        Long amount = cancelAmountNum.longValue();
        if (status.equals("CANCELED")) {
            // 부분 취소 입금
            // 토스에서 제공하는 환불 금액으로 실제 지갑 증액
            if (!amount.equals(payment.getAmount())) {
                if(payment.updatePaymentRefundedAmount(amount)) {
                    payment.updatePaymentStatus(PaymentStatus.PARTIALLY_CANCELED);
                    wallet.depositBalance(amount);
                    walletRepository.save(wallet);
                    paymentRepository.save(payment);
                    wallet.createBalanceLogEvent(req.amount(), EventType.부분취소_입금);
                    log.info("토스 부분 환불 완료 - orderId={}, refundAmount={}", req.orderId(), amount);
                }
            } else {
                // 전액 환불
                if(payment.updatePaymentRefundedAmount(amount)) {
                    payment.updatePaymentStatus(PaymentStatus.CANCELED);
                    wallet.depositBalance(amount);
                    walletRepository.save(wallet);
                    paymentRepository.save(payment);
                    wallet.createBalanceLogEvent(payment.getAmount(), EventType.전체취소_입금);
                    log.info("토스 전액 환불 완료 - orderId={}, refundAmount={}", req.orderId(), amount);
                }
            }
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

    /**
     * 내부 결제 취소 기능
     **/
    @Retryable(
            retryFor = OptimisticLockException.class,
            maxAttempts = 3
    )
    @Transactional
    public void cancelPayment(PaymentCancelRequestDto req){
        // 결제 확인
        Payment payment = paymentRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> {
                    log.error("결제 조회 실패 - orderId={}", req.orderId());
                    return new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
                });
        // 지갑 확인
        Wallet wallet = walletRepository.findByHolderId(payment.getBuyer().getId())
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - memberId={}", payment.getBuyer().getId());
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });

        // 상태 체크 - 유저가 부분취소를 여러번 할 수 있음
        if (payment.getStatus() == PaymentStatus.CANCELED) {
            log.error("이미 취소된 결제입니다 - orderId={}", req.orderId());
            throw new CustomException(ErrorCode.PAYMENT_NOT_COMPLETE);
        }

        // 1. 0원 / 음수 방지
        if (req.amount() <= 0) {
            log.error("환불 금액이 유효하지 않습니다 - orderId={}, amount={}", req.orderId(), req.amount());
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT);
        }

        // 3. 전액 환불
        if (req.amount().equals(payment.getAmount())) {
            if (payment.updatePaymentRefundedAmount(req.amount())) {
                payment.updatePaymentStatus(PaymentStatus.CANCELED);
                paymentRepository.save(payment);
                wallet.depositBalance(req.amount());
                walletRepository.save(wallet);
                wallet.createBalanceLogEvent(req.amount(), EventType.전체취소_입금);
                log.info("내부 결제 전액 환불 완료 - orderId={}, refundAmount={}", req.orderId(), req.amount());
            }
            // 4. 부분 환불
            else {
                if (payment.updatePaymentRefundedAmount(req.amount())) {
                    payment.updatePaymentStatus(PaymentStatus.PARTIALLY_CANCELED);
                    paymentRepository.save(payment);
                    wallet.depositBalance(req.amount());
                    walletRepository.save(wallet);
                    wallet.createBalanceLogEvent(req.amount(), EventType.부분취소_입금);
                    log.info("내부 결제 부분 환불 완료 - orderId={}, refundAmount={}", req.orderId(), req.amount());
                }
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
}
