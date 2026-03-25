package com.thock.back.market.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderCancelHistory;
import com.thock.back.market.domain.OrderItem;
import com.thock.back.market.out.repository.OrderCancelHistoryRepository;
import com.thock.back.shared.market.domain.CancelReasonType;
import com.thock.back.market.out.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class MarketCancelOrderPaymentUseCase {
    private final OrderRepository orderRepository;
    private final OrderCancelHistoryRepository orderCancelHistoryRepository;

    @Transactional
    public void cancelOrder(Long memberId, Long orderId, CancelReasonType cancelReasonType, String cancelReasonDetail){
        Order order = getOwnedOrder(memberId, orderId);

        // 3. 도메인 메서드 호출 (이벤트 발행은 도메인이 처리)
        if (order.isPaymentInProgress()) {
            order.cancelRequestPayment(cancelReasonType, cancelReasonDetail);
        } else {
            order.cancel(cancelReasonType, cancelReasonDetail);
        }

        // 4. 취소 히스토리 저장 (각 아이템별로)
        List<OrderCancelHistory> histories = buildUserCancelHistories(
                order,
                order.getItems(),
                cancelReasonType,
                cancelReasonDetail
        );
        orderCancelHistoryRepository.saveAll(histories);
    }

    // 부분 취소
    @Transactional
    public void cancelOrderItems(
            Long memberId,
            Long orderId,
            List<Long> orderItemIds,
            CancelReasonType cancelReasonType,
            String cancelReasonDetail)
    {
        Order order = getOwnedOrder(memberId, orderId);

        // 3. 취소할 아이템 조회 (히스토리 저장용) - 취소 대상 아이템들 미리 확보
        List<OrderItem> itemsToCancel = order.getItems().stream()
                        .filter(item -> orderItemIds.contains(item.getId()))
                        .toList();

        // 4. 도메인 메서드 호출 (도메인에서 상태 변경, 이벤트 발행)
        order.cancelItems(orderItemIds, cancelReasonType, cancelReasonDetail);

        // 5. 취소 히스토리 저장 (취소된 아이템들만)
        List<OrderCancelHistory> histories = buildUserCancelHistories(
                order,
                itemsToCancel,
                cancelReasonType,
                cancelReasonDetail
        );
        orderCancelHistoryRepository.saveAll(histories);
    }

    private Order getOwnedOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getBuyer().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.ORDER_USER_FORBIDDEN);
        }
        return order;
    }

    private List<OrderCancelHistory> buildUserCancelHistories(
            Order order,
            List<OrderItem> items,
            CancelReasonType cancelReasonType,
            String cancelReasonDetail
    ) {
        return items.stream()
                .map(item -> OrderCancelHistory.ofUserCancel(order, item, cancelReasonType, cancelReasonDetail))
                .toList();
    }
}
