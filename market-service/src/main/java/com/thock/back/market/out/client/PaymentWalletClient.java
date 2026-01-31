package com.thock.back.market.out.client;


import com.thock.back.market.out.api.dto.WalletInfo;

// Outbount Port(인터페이스)
public interface PaymentWalletClient {
    WalletInfo getWallet(Long memberId);
}
