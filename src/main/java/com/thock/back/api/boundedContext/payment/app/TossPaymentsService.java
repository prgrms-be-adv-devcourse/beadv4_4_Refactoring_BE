package com.thock.back.api.boundedContext.payment.app;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class TossPaymentsService {

    private static final String TOSS_BASE_URL = "https://api.tosspayments.com";
    private static final String CONFIRM_PATH = "/v1/payments/confirm";

    private final RestClient tossRestClient;
    private final ObjectMapper objectMapper;

    @Value("${custom.payment.toss.payments.secretKey:}")
    private String tossSecretKey;

    public TossPaymentsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.tossRestClient = RestClient.builder()
                .baseUrl(TOSS_BASE_URL)
                .build();
    }

    public Map<String, Object> confirmCardPayment(String paymentKey, String orderId, long amount) {
        TossPaymentsConfirmRequest requestBody = new TossPaymentsConfirmRequest(
                paymentKey,
                orderId,
                amount
        );

        try {
            ResponseEntity<Map> responseEntity = createConfirmRequest(requestBody)
                    .retrieve()
                    .toEntity(Map.class);

            int httpStatus = responseEntity.getStatusCode().value();
            Map<String, Object> responseBody = responseEntity.getBody();

            if (httpStatus != 200) {
                throw createTossExceptionFromNon200(httpStatus, responseBody);
            }

            if (responseBody == null) {
                throw new CustomException(ErrorCode.TOSS_EMPTY_RESPONSE);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) responseBody;
            return casted;

        } catch (RestClientResponseException e) {
            throw createTossExceptionFromHttpError(e);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.TOSS_CALL_EXCEPTION);
        }
    }

    private RestClient.RequestHeadersSpec<?> createConfirmRequest(TossPaymentsConfirmRequest requestBody) {
        return tossRestClient.post()
                .uri(CONFIRM_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth(tossSecretKey, ""))
                .body(requestBody);
    }

    private CustomException createTossExceptionFromNon200(int httpStatus, Map body) {
        if (body == null) {
            return new CustomException(ErrorCode.TOSS_EMPTY_RESPONSE);
        }

        String tossCode = extractStringOrDefault(body, "code", "HTTP_" + httpStatus);
        String tossMessage = extractStringOrDefault(body, "message", "결제 승인 실패");

        return new CustomException(
                ErrorCode.TOSS_CONFIRM_FAIL,
                "[%s] %s".formatted(tossCode, tossMessage)
        );
    }

    private CustomException createTossExceptionFromHttpError(RestClientResponseException e) {
        int httpStatus = e.getStatusCode().value();
        String rawBody = e.getResponseBodyAsString(StandardCharsets.UTF_8);

        if (rawBody == null || rawBody.isBlank()) {
            return new CustomException(ErrorCode.TOSS_EMPTY_RESPONSE);
        }

        try {
            Map<String, Object> err = objectMapper.readValue(rawBody, new TypeReference<>() {});
            String tossCode = extractStringOrDefault(err, "code", "HTTP_" + httpStatus);
            String tossMessage = extractStringOrDefault(err, "message", "결제 승인 실패");

            return new CustomException(
                    ErrorCode.TOSS_HTTP_ERROR,
                    "[%s] %s".formatted(tossCode, tossMessage)
            );

        } catch (Exception parseFail) {
            return new CustomException(
                    ErrorCode.TOSS_UNKNOWN_CODE,
                    "HTTP %s / raw=%s".formatted(httpStatus, rawBody)
            );
        }
    }

    private String extractStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String s && !s.isBlank()) return s;
        return defaultValue;
    }

    public record TossPaymentsConfirmRequest(String paymentKey, String orderId, long amount) {
    }
}