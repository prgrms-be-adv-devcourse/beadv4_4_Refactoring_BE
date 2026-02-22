package com.thock.back.market.out.client;

import com.thock.back.market.out.api.dto.WalletInfo;
import com.thock.back.market.out.client.fallback.PaymentWalletClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "payment-wallet-client",
        url = "${custom.global.paymentServiceUrl}/api/v1/payments/internal",
        fallbackFactory = PaymentWalletClientFallbackFactory.class
)
public interface PaymentWalletClient {
    @GetMapping("/wallets/{memberId}")
    WalletInfo getWallet(@PathVariable Long memberId);
}
