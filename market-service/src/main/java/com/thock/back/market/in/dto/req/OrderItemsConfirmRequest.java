package com.thock.back.market.in.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "주문 상품 부분 구매 확정 요청")
public record OrderItemsConfirmRequest (
        @Schema(description = "구매 확정할 주문 상품 ID 목록", example = "[1, 2]")
        @NotEmpty(message = "구매 확정할 상품을 선택해주세요")
        List<@NotNull Long> orderItemIds
){
}
