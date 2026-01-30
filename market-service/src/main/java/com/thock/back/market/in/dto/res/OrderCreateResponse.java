package com.thock.back.market.in.dto.res;

import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderItem;
import com.thock.back.market.domain.OrderItemState;
import com.thock.back.market.domain.OrderState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "주문 생성 응답")
public record OrderCreateResponse (
        @Schema(description = "주문 ID", example = "1")
        Long orderId,

        @Schema(description = "주문 번호", example = "ORDER-20250120-A1B2C3D4")
        String orderNumber,

        @Schema(description = "주문 상태", example = "PENDING_PAYMENT")
        OrderState state,

        @Schema(description = "주문 아이템 목록")
        List<OrderItemInfo> items,

        @Schema(description = "총 정가", example = "300000")
        Long totalPrice,

        @Schema(description = "총 판매가", example = "240000")
        Long totalSalePrice,

        @Schema(description = "총 할인 금액", example = "60000")
        Long totalDiscountAmount,

        @Schema(description = "PG 결제 금액 (총 판매가 - 예치금)", example = "200000")
        Long pgAmount,

        @Schema(description = "배송지 - 우편번호", example = "06234")
        String zipCode,

        @Schema(description = "배송지 - 기본 주소", example = "서울특별시 강남구 테헤란로 123")
        String baseAddress,

        @Schema(description = "배송지 - 상세 주소", example = "ABC빌딩 4층")
        String detailAddress,

        @Schema(description = "주문 생성 시간")
        LocalDateTime createdAt

)
    {
    // 정적 팩토리 메서드
    public static OrderCreateResponse from(Order order, Long pgAmount) {
        List<OrderItemInfo> itemInfos = order.getItems().stream()
                .map(OrderItemInfo::from)
                .collect(Collectors.toList());

        return new OrderCreateResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getState(),
                itemInfos,
                order.getTotalPrice(),
                order.getTotalSalePrice(),
                order.getTotalDiscountAmount(),
                pgAmount,
                order.getZipCode(),
                order.getBaseAddress(),
                order.getDetailAddress(),
                order.getCreatedAt()
        );
    }

    @Schema(description = "주문 아이템 정보")
    public record OrderItemInfo (
            @Schema(description = "주문 아이템 ID", example = "1")
            Long orderItemId,

            @Schema(description = "판매자 ID")
            Long sellerId,

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

            @Schema(description = "상품 상태")
            OrderItemState state,

            @Schema(description = "총 정가", example = "300000")
            Long totalPrice,

            @Schema(description = "총 판매가", example = "240000")
            Long totalSalePrice,

            @Schema(description = "할인 금액", example = "60000")
            Long discountAmount

    ) {
        public static OrderItemInfo from(OrderItem orderItem) {
            return new OrderItemInfo(
                    orderItem.getId(),
                    orderItem.getSellerId(),
                    orderItem.getProductId(),
                    orderItem.getProductName(),
                    orderItem.getProductImageUrl(),
                    orderItem.getPrice(),
                    orderItem.getSalePrice(),
                    orderItem.getQuantity(),
                    orderItem.getState(),
                    orderItem.getTotalPrice(),
                    orderItem.getTotalSalePrice(),
                    orderItem.getDiscountAmount()
            );
        }
    }
}
