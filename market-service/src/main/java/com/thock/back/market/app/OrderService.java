package com.thock.back.market.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import java.util.List;
import com.thock.back.market.domain.Order;
import com.thock.back.market.in.dto.res.OrderDetailResponse;
import com.thock.back.market.out.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * 내 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getMyOrders(Long memberId) {
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(memberId);

        return orders.stream()
                .map(OrderDetailResponse::from)
                .toList();
    }

    /**
     * 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getBuyer().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.ORDER_USER_FORBIDDEN);
        }

        return OrderDetailResponse.from(order);
    }
}
