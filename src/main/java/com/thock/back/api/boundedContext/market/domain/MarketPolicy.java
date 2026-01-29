package com.thock.back.api.boundedContext.market.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MarketPolicy {
    public static Double PRODUCT_PAYOUT_RATE;

    // 플랫폼(서비스)이 가져갈 수수료 계산
    public static Long calculatePayoutFee(Long salePrice, Double payoutRate) {
        return salePrice - calculateSalePriceWithoutFee(salePrice, payoutRate);
    }

    // 판매자가 실제로 받을 금액 계산 (수수료 제외)
    public static Long calculateSalePriceWithoutFee(Long salePrice, Double payoutRate) {
        return Math.round(salePrice * payoutRate / 100);
    }

    @Value("${custom.market.product.payoutRate}")
    public void setProductPayoutRate(Double rate) {
        PRODUCT_PAYOUT_RATE = rate;
    }
}
