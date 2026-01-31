package com.thock.back.market.in.dto.res;

import com.thock.back.market.domain.CartItem;
import com.thock.back.market.out.api.dto.ProductInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 상품 정보 응답")
public record CartItemResponse (
        // CartItem 정보
        @Schema(description = "장바구니에 등록된 상품 ID", example = "1")
        Long cartItemId,
        @Schema(description = "상품 수량", example = "3")
        Integer quantity,

        // Product 정보
        @Schema(description = "상품 ID", example = "100")
        Long productId,
        @Schema(description = "상품명", example = "기계식 키보드")
        String productName,
        @Schema(description = "상품 이미지 URL", example = "https://example.com/images/keyboard.jpg")
        String productImageUrl,
        @Schema(description = "상품 정가 (단가)", example = "150000")
        Long price,
        @Schema(description = "상품 판매가 (할인가, 단가)", example = "120000")
        Long salePrice,
        @Schema(description = "재고 수량", example = "50")
        Integer stock,
        //    private boolean state; // TODO : 상품의 상태 -> ERD에 ENUM으로 되어있음.

        /**
         * 상품 1개에 대한 총 가격
         * 15000원 짜리 기계식 키보드가 12000원에 판매중
         * 4개 구매
         */
        @Schema(description = "총 정가 (수량 * 정가)", example = "60000")
        Long totalPrice,        // quantity * price
        @Schema(description = "총 판매가 (수량 * 판매가)", example = "48000")
        Long totalSalePrice,    // quantity * salePrice
        @Schema(description = "할인 금액 (총 정가 - 총 판매가)", example = "12000")
        Long totalDiscountAmount    // totalPrice - totalSalePrice
)
{
    public static CartItemResponse from(CartItem item, ProductInfo product) {
        Long totalPrice = item.getQuantity() * product.getPrice();
        Long totalSalePrice = item.getQuantity() * product.getSalePrice();
        Long totalDiscountAmount = totalPrice - totalSalePrice;

        return new CartItemResponse(
                item.getId(),
                item.getQuantity(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                product.getSalePrice(),
                product.getStock(),
//                .isAvailable(product.isAvailable())
                totalPrice,
                totalSalePrice,
                totalDiscountAmount
                );
    }
}
