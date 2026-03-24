package com.thock.back.market.in.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "장바구니 상품 추가 요청")
public record CartItemAddRequest (
        @Schema(description = "상품 ID", example = "1")
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,
        @Schema(description = "수량", example = "2")
        @NotNull(message = "수량은 필수 입니다.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        Integer quantity
){ }
