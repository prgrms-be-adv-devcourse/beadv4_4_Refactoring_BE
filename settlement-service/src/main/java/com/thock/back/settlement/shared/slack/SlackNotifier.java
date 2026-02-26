package com.thock.back.settlement.shared.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackNotifier {

    private final WebClient.Builder webClientBuilder;

    @Value("${slack.webhook-url:}")
    private String webhookUrl;

    public void notifyMonthlySettlementFailure(Long sellerId, String targetYearMonth, int retryCount, String reason) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[Slack] webhook-url is empty. skip monthly settlement failure alert.");
            return;
        }

        String text = """
                :rotating_light: 월별 정산 최종 실패(재시도 한도 초과)
                - service: settlement-service
                - sellerId: %s
                - targetYearMonth: %s
                - retryCount: %s
                - reason: %s
                """.formatted(sellerId, targetYearMonth, retryCount, sanitize(reason));

        try {
            webClientBuilder.build()
                    .post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            log.error("[Slack] monthly settlement failure alert send failed. sellerId={}, targetYearMonth={}",
                    sellerId, targetYearMonth, e);
        }
    }

    private String sanitize(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }
        String singleLine = reason.replace('\n', ' ').replace('\r', ' ');
        return singleLine.length() > 300 ? singleLine.substring(0, 300) + "..." : singleLine;
    }
}
