package com.thock.back.api.boundedContext.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    KEYBOARD("키보드");
    private final String description;
}
