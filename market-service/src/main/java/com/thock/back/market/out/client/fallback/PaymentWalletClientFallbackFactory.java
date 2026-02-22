package com.thock.back.market.out.client.fallback;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.out.client.PaymentWalletClient;
import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class PaymentWalletClientFallbackFactory implements FallbackFactory<PaymentWalletClient> {

    @Override
    public PaymentWalletClient create(Throwable cause) {
        final Throwable root = unwrap(cause);

        log.warn("[PaymentWalletClient Fallback] cause={}, root={}",
                cause.getClass().getSimpleName(),
                root.getClass().getSimpleName(),
                root);

        return memberId -> {
            if (root instanceof CustomException ce) throw ce;
            /**
             * 사실 지금은 필요 없는데 확장용
             * - 나중에 ErrorDecoder에서 CustomException으로 변환할 때
             * - 다른 래핑/재호출 경로에서 이미 CustomException이 들어올 때
             *
             * 왜 필요 없냐면 "서비스 내부 예외 타입은 HTTP 넘어오면 사라진다"
             * payment에서 CustomException을 던져도 market에서는 Java 예외로 받는게 아니라 HTTP 응답으로 받음
             * FeignException으로 받는다는 얘기임
             */

            // 서킷 OPEN 차단
            if (root instanceof CallNotPermittedException) {
                throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
            }

            // 네트워크/타임아웃/재시도 실패 계열
            if (root instanceof RetryableException) {
                throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
            }

            // HTTP 에러는 status로 분기
            if (root instanceof FeignException fe) {
                int status = fe.status();
                if (status >= 500) {
                    throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
                }
                // 4xx는 일반 실패로 처리
                throw new CustomException(ErrorCode.ORDER_WALLET_API_FAILED);
            }

            // 그 외는 일반 실패로 처리
            throw new CustomException(ErrorCode.ORDER_WALLET_API_FAILED);
        };
    }

    // Feign이 내부적으로 예외를 감쌀 때 실제 원인을 꺼내는 로직
    private Throwable unwrap(Throwable t) {
        Throwable cur = t;
        while (true) {
            if (cur instanceof CompletionException || cur instanceof ExecutionException) {
                if (cur.getCause() == null) return cur;
                cur = cur.getCause();
                continue;
            }
            if (cur instanceof UndeclaredThrowableException ute) {
                if (ute.getUndeclaredThrowable() == null) return cur;
                cur = ute.getUndeclaredThrowable();
                continue;
            }
            return cur;
        }
    }
}
