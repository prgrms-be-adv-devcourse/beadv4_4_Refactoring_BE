package com.thock.back.api.boundedContext.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductState {
    SOLDOUT("품절"),
    ON_SALE("판매중"),
    STOPPED("판매중지");

    private final String description;
}
