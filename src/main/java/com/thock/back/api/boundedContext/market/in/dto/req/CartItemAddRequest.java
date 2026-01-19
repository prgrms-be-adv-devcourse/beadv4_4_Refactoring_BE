package com.thock.back.api.boundedContext.market.in.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "장바구니 상품 추가 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemAddRequest {
    @Schema(description = "상품 ID", example = "5")
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @Schema(description = "수량", example = "2")
    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private Integer quantity;
}
