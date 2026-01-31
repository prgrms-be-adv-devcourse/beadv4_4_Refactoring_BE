package com.thock.back.market.out.api;


import com.thock.back.market.out.api.dto.WalletInfo;
import com.thock.back.market.out.client.PaymentWalletClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Outbound Adapter (구현체)
@Component
public class PaymentWalletApiClient implements PaymentWalletClient {
    private final RestClient restClient;

    public PaymentWalletApiClient(@Value("${custom.global.paymentServiceUrl}") String paymentServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentServiceUrl + "/api/v1/payments/internal")
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
