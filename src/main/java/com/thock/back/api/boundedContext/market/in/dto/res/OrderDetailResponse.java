package com.thock.back.api.boundedContext.market.in.dto.res;

import com.thock.back.api.boundedContext.market.domain.Order;
import com.thock.back.api.boundedContext.market.domain.OrderItem;
import com.thock.back.api.boundedContext.market.domain.OrderItemState;
import com.thock.back.api.boundedContext.market.domain.OrderState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse (
        @Schema(description = "주문 ID", example = "1")
        Long orderId,

        @Schema(description = "주문 번호", example = "ORDER-20250129-A1B2C3D4")
        String orderNumber,

        @Schema(description = "주문 상태", example = "PAYMENT_COMPLETED")
        OrderState state,

        @Schema(description = "총 정가", example = "300000")
        Long totalPrice,

        @Schema(description = "총 판매가", example = "240000")
        Long totalSalePrice,

        @Schema(description = "총 할인 금액", example = "60000")
        Long totalDiscountAmount,

        @Schema(description = "배송지 - 우편번호", example = "06234")
        String zipCode,

        @Schema(description = "배송지 - 기본 주소", example = "서울특별시 강남구 테헤란로 123")
        String baseAddress,

        @Schema(description = "배송지 - 상세 주소", example = "ABC빌딩 4층")
        String detailAddress,

        @Schema(description = "주문 생성 시간", example = "2025-01-29T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "결제 완료 시간", example = "2025-01-29T14:35:00")
        LocalDateTime paymentDate,

        @Schema(description = "주문 상품 목록")
        List<OrderItemDto> items
)
{

    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getState(),
                order.getTotalPrice(),
                order.getTotalSalePrice(),
                order.getTotalDiscountAmount(),
                order.getZipCode(),
                order.getBaseAddress(),
                order.getDetailAddress(),
                order.getCreatedAt(),
                order.getPaymentDate(),
                order.getItems().stream()
                        .map(OrderItemDto::from)
                        .toList());
    }

    public record OrderItemDto (
            @Schema(description = "주문 상품 ID", example = "1")
            Long orderItemId,

            @Schema(description = "상품 ID", example = "100")
            Long productId,

            @Schema(description = "상품명", example = "기계식 키보드")
            String productName,

            @Schema(description = "상품 이미지 URL", example = "https://example.com/images/keyboard.jpg")
            String productImageUrl,

            @Schema(description = "정가 (단가)", example = "150000")
            Long price,

            @Schema(description = "판매가 (단가)", example = "120000")
            Long salePrice,

            @Schema(description = "수량", example = "2")
            Integer quantity,

            @Schema(description = "주문 상품 상태", example = "PAYMENT_COMPLETED")
            OrderItemState state,

            @Schema(description = "총 판매가 (수량 × 판매가)", example = "240000")
            Long totalSalePrice
    )
    {
        public static OrderItemDto from(OrderItem item) {
            return new OrderItemDto(
                    item.getId(),
                    item.getProductId(),
                    item.getProductName(),
                    item.getProductImageUrl(),
                    item.getPrice(),
                    item.getSalePrice(),
                    item.getQuantity(),
                    item.getState(),
                    item.getTotalSalePrice()
                    );
        }
    }
}
