package com.thock.back.api.boundedContext.market.in.dto.res;

import com.thock.back.api.boundedContext.market.domain.Order;
import com.thock.back.api.boundedContext.market.domain.OrderItem;
import com.thock.back.api.boundedContext.market.domain.OrderState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "주문 생성 응답")
@Getter
@AllArgsConstructor
public class OrderCreateResponse {
    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORDER-20250120-A1B2C3D4")
    private String orderNumber;

    @Schema(description = "주문 상태", example = "PENDING_PAYMENT")
    private OrderState state;

    @Schema(description = "주문 아이템 목록")
    private List<OrderItemInfo> items;

    @Schema(description = "총 정가", example = "300000")
    private Long totalPrice;

    @Schema(description = "총 판매가", example = "240000")
    private Long totalSalePrice;

    @Schema(description = "총 할인 금액", example = "60000")
    private Long totalDiscountAmount;

    @Schema(description = "총 정산 금액 (판매자가 받을 금액)", example = "192000")
    private Long totalPayoutAmount;

    @Schema(description = "총 수수료 (플랫폼 수익)", example = "48000")
    private Long totalFeeAmount;

    @Schema(description = "배송지 - 우편번호", example = "06234")
    private String zipCode;

    @Schema(description = "배송지 - 기본 주소", example = "서울특별시 강남구 테헤란로 123")
    private String baseAddress;

    @Schema(description = "배송지 - 상세 주소", example = "ABC빌딩 4층")
    private String detailAddress;

    @Schema(description = "주문 생성 시간")
    private LocalDateTime createdAt;

    // 정적 팩토리 메서드
    public static OrderCreateResponse from(Order order) {
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
                order.getTotalPayoutAmount(),
                order.getTotalFeeAmount(),
                order.getZipCode(),
                order.getBaseAddress(),
                order.getDetailAddress(),
                order.getCreatedAt()
        );
    }

    @Schema(description = "주문 아이템 정보")
    @Getter
    @AllArgsConstructor
    public static class OrderItemInfo {
        @Schema(description = "주문 아이템 ID", example = "1")
        private Long orderItemId;

        @Schema(description = "상품 ID", example = "100")
        private Long productId;

        @Schema(description = "상품명", example = "기계식 키보드")
        private String productName;

        @Schema(description = "상품 이미지 URL", example = "https://example.com/images/keyboard.jpg")
        private String productImageUrl;

        @Schema(description = "정가 (단가)", example = "150000")
        private Long price;

        @Schema(description = "판매가 (단가)", example = "120000")
        private Long salePrice;

        @Schema(description = "수량", example = "2")
        private Integer quantity;

        @Schema(description = "총 정가", example = "300000")
        private Long totalPrice;

        @Schema(description = "총 판매가", example = "240000")
        private Long totalSalePrice;

        @Schema(description = "할인 금액", example = "60000")
        private Long discountAmount;

        public static OrderItemInfo from(OrderItem orderItem) {
            return new OrderItemInfo(
                    orderItem.getId(),
                    orderItem.getProductId(),
                    orderItem.getProductName(),
                    orderItem.getProductImageUrl(),
                    orderItem.getPrice(),
                    orderItem.getSalePrice(),
                    orderItem.getQuantity(),
                    orderItem.getTotalPrice(),
                    orderItem.getTotalSalePrice(),
                    orderItem.getDiscountAmount()
            );
        }
    }
}
