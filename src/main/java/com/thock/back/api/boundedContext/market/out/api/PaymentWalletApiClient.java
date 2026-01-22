package com.thock.back.api.boundedContext.market.out.api;

import com.thock.back.api.boundedContext.market.out.api.dto.WalletInfo;
import com.thock.back.api.boundedContext.market.out.client.PaymentWalletClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Outbound Adapter (구현체)
@Component
public class PaymentWalletApiClient implements PaymentWalletClient {
    private final RestClient restClient;

    public PaymentWalletApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/payments/internal")
                .build();
    }

    @Override
    public WalletInfo getWallet(Long memberId) {
        return restClient.get()
                .uri("/wallets/{memberId}", memberId)
                .retrieve()
                .body(WalletInfo.class);
    }
}
