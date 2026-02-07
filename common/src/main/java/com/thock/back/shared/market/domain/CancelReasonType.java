package com.thock.back.shared.market.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 구매자의 취소 사유
@Getter
@RequiredArgsConstructor
public enum CancelReasonType {
    CHANGE_OF_MIND("단순 변심"),
    DELIVERY_DELAY("배송 지연"),
    PRODUCT_DEFECT("상품 불량"),
    WRONG_OPTION("옵션 잘못 선택"),
    ETC("기타");

    private final String description;

}
