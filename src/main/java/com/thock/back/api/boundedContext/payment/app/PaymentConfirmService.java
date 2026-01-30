package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.*;
import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
import com.thock.back.api.shared.payment.dto.PaymentCancelRequestDto;
import com.thock.back.api.boundedContext.payment.domain.dto.request.PaymentConfirmRequestDto;
import com.thock.back.api.boundedContext.payment.out.PaymentRepository;
import com.thock.back.api.boundedContext.payment.out.WalletRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.shared.payment.dto.PaymentDto;
import com.thock.back.api.shared.payment.dto.RefundResponseDto;
import com.thock.back.api.shared.payment.event.PaymentCompletedEvent;
import com.thock.back.api.shared.payment.event.PaymentRefundCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
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
    @Transactional
    public Map<String, Object> confirmPayment(PaymentConfirmRequestDto req) {
        Payment payment = paymentRepository.findByOrderId(req.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER));
        PaymentMember member = paymentMemberRepository.getReferenceById(payment.getBuyer().getId());
        Wallet wallet = walletRepository.findByHolderId(member.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
        // 상태 체크
        if (payment.getStatus() != PaymentStatus.REQUESTED) {
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
                .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class).map(msg -> {
                    throw new CustomException(ErrorCode.TOSS_CONFIRM_FAIL);
                }))
                .bodyToMono(Map.class)
                .block();

        // confirm 결과 검증 (토스 응답)
        Integer approvedAmount = (Integer) confirmResponse.get("totalAmount");
        if (!payment.getPgAmount().equals(approvedAmount.longValue())) {
            payment.updatePaymentStatus(PaymentStatus.CANCELED);
            throw new CustomException(ErrorCode.TOSS_AMOUNT_NOT_MATCH);
        }

        // 검증 후 process
        payment.updatePaymentStatus(PaymentStatus.COMPLETED);
        payment.updatePaymentKey(req.getPaymentKey());
        paymentRepository.save(payment);
        payment.createPaymentLogEvent();

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
    @Transactional
    public void cancelToss(PaymentCancelRequestDto req){
        // 검증
        Payment payment = paymentRepository.findByOrderId(req.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER));

        Wallet wallet = walletRepository.findByHolderId(payment.getBuyer().getId()).get();

        // 상태 체크
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_COMPLETE);
        }

        Map<String, Object> body = Map.of(
                "paymentKey", payment.getPaymentKey(),
                "cancelReason", req.getCancelReason()
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
                .uri(CANCEL_PATH, payment.getPaymentKey())
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class).map(msg -> {
                    throw new CustomException(ErrorCode.TOSS_CONFIRM_FAIL);
                }))
                .bodyToMono(Map.class)
                .block();

        // cancel 결과 검증 (토스 응답)
        String status = (String) confirmResponse.get("status");
        if (status.equals("CANCELED")) {
            payment.updatePaymentStatus(PaymentStatus.CANCELED);
            paymentRepository.save(payment);
        }

        // 지갑 업데이트
        wallet.depositBalance(payment.getAmount());
        walletRepository.save(wallet);
        wallet.createBalanceLogEvent(payment.getAmount(), EventType.주문취소_입금);

        eventPublisher.publish(
                new PaymentRefundCompletedEvent(
                        new RefundResponseDto(
                                payment.getBuyer().getId(),
                                payment.getOrderId()
                        )
                )
        );
    }

    /**
     * 내부 결제 취소 기능
     **/
    @Transactional
    public void cancelPayment(PaymentCancelRequestDto req){
        // 검증
        Payment payment = paymentRepository.findByOrderId(req.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER));

        Wallet wallet = walletRepository.findByHolderId(payment.getBuyer().getId()).get();

        // 상태 체크
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_COMPLETE);
        }

        // cancel 결과 검증
        payment.updatePaymentStatus(PaymentStatus.CANCELED);
        paymentRepository.save(payment);


        // 지갑 업데이트
        wallet.depositBalance(payment.getAmount());
        walletRepository.save(wallet);
        wallet.createBalanceLogEvent(payment.getAmount(), EventType.주문취소_입금);

        eventPublisher.publish(
                new PaymentRefundCompletedEvent(
                        new RefundResponseDto(
                                payment.getBuyer().getId(),
                                payment.getOrderId()
                        )
                )
        );
    }
}
