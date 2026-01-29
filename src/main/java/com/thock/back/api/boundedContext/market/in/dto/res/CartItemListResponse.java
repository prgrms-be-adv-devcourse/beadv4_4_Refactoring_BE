package com.thock.back.api.boundedContext.market.in.dto.res;

import com.thock.back.api.boundedContext.market.domain.Cart;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.List;

@Schema(description = "장바구니 전체 조회 응답")
public record CartItemListResponse (
        @Schema(description = "장바구니 ID", example = "1")
        Long cartId,

        @Schema(description = "장바구니 상품 목록")
        List<CartItemResponse> items,

        @Schema(description = "장바구니 총 상품 개수", example = "3")
        Integer totalItemCount,

        @Schema(description = "전체 정가 합계 (할인 전)", example = "300000")
        Long totalPrice,

        @Schema(description = "전체 판매가 합계 (할인 후)", example = "240000")
        Long totalSalePrice,

        @Schema(description = "전체 할인 금액", example = "60000")
        Long totalDiscountAmount
)
{
        public static CartItemListResponse from(Cart cart, List<CartItemResponse> items){
                Integer totalItemCount = items.size();
                Long totalPrice = items.stream()
                        .mapToLong(CartItemResponse::totalPrice)
                        .sum();
                Long totalSalePrice = items.stream()
                        .mapToLong(CartItemResponse::totalSalePrice)
                        .sum();
                Long totalDiscountAmount = totalPrice - totalSalePrice;

                return new CartItemListResponse(
                        cart.getId(),
                        items,
                        totalItemCount,
                        totalPrice,
                        totalSalePrice,
                        totalDiscountAmount
                );
        }

}
