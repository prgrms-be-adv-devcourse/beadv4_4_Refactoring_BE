package com.thock.back.market.out.client.fallback;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.out.client.ProductClient;
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
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        final Throwable root = unwrap(cause);

        // 원인 로그(운영)
        log.warn("[ProductClient Fallback] cause={}, root={}",
                cause.getClass().getSimpleName(),
                root.getClass().getSimpleName(),
                root);

        return productIds -> {
            if (root instanceof CustomException ce) throw ce;
            /** ⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆⬆
             * 사실 지금은 필요 없는데 확장용
             * - 나중에 ErrorDecoder에서 CustomException으로 변환할 때
             * - 다른 래핑/재호출 경로에서 이미 CustomException이 들어올 때
             *
             * 왜 필요 없냐면 "서비스 내부 예외 타입은 HTTP 넘어오면 사라진다"
             * product에서 CustomException을 던져도 market에서는 Java 예외로 받는게 아니라 HTTP 응답 으로 받음
             * FeignException으로 받는다는 얘기임
             *
             * 정리하자면
             * - HTTP 4xx/5xx 응답 -> FeignException
             * - 연결 실패/타임아웃 -> RetryableException (또는 하위 원인)
             * - CB OPEN 차단 -> CallNotPermittedException (fallback에서 받음)
             * -> fallback이 없으면 NoFallbackAvailableException를 던짐
             */

            // 서킷 OPEN 차단
            if (root instanceof CallNotPermittedException) {
                throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
            }

            // 네트워크/타임아웃/재시도 실패 계열
            if (root instanceof RetryableException) {
                throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
            }

            // FeignException 상태코드 분기 - HTTP 에러는 status로 분기(선택)
            if (root instanceof FeignException fe) {
                int status = fe.status();
                if (status >= 500) {
                    throw new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
                }
                // 4xx를 어떻게 할지는 정책(예: NOT_FOUND 등)
                // 필요하면 status별로 ErrorCode 매핑
                throw new CustomException(ErrorCode.CART_PRODUCT_API_FAILED);
            }

            // 그 외는 일반 실패로 처리
            throw new CustomException(ErrorCode.CART_PRODUCT_API_FAILED);
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
            // 추가로 래핑되는 타입 있으면 여기에 계속 추가 가능
            return cur;
        }
    }
}
