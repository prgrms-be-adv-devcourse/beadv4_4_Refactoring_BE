package com.thock.back.api.boundedContext.market.in.dto.res;

import com.thock.back.api.boundedContext.market.domain.Order;
import com.thock.back.api.boundedContext.market.domain.OrderItem;
import com.thock.back.api.boundedContext.market.domain.OrderItemState;
import com.thock.back.api.boundedContext.market.domain.OrderState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class OrderDetailResponse {

    private Long orderId;
    private String orderNumber;
    private OrderState state;

    private Long totalPrice;
    private Long totalSalePrice;
    private Long totalDiscountAmount;

    private String zipCode;
    private String baseAddress;
    private String detailAddress;

    private LocalDateTime createdAt;
    private LocalDateTime paymentDate;

    private List<OrderItemDto> items;

    public static OrderDetailResponse from(Order order) {
        return OrderDetailResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .state(order.getState())
                .totalPrice(order.getTotalPrice())
                .totalSalePrice(order.getTotalSalePrice())
                .totalDiscountAmount(order.getTotalDiscountAmount())
                .zipCode(order.getZipCode())
                .baseAddress(order.getBaseAddress())
                .detailAddress(order.getDetailAddress())
                .createdAt(order.getCreatedAt())
                .paymentDate(order.getPaymentDate())
                .items(order.getItems().stream()
                        .map(OrderItemDto::from)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderItemDto {
        private Long orderItemId;
        private Long productId;
        private String productName;
        private String productImageUrl;
        private Long price;
        private Long salePrice;
        private Integer quantity;
        private OrderItemState state;
        private Long totalSalePrice;

        public static OrderItemDto from(OrderItem item) {
            return OrderItemDto.builder()
                    .orderItemId(item.getId())
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .productImageUrl(item.getProductImageUrl())
                    .price(item.getPrice())
                    .salePrice(item.getSalePrice())
                    .quantity(item.getQuantity())
                    .state(item.getState())
                    .totalSalePrice(item.getTotalSalePrice())
                    .build();
        }
    }
}
