package com.thock.back.settlement.reconciliation.app.port;

public record SettlementCandidate(
        Long salesLogId,
        Long sellerId,
        Long productId,
        String productName,
        int productQuantity,
        long paymentAmount,
        String orderNo
) {
    public void toEntity(){

    }
}