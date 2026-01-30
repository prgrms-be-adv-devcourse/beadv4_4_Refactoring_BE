package com.thock.back.market.app;


import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Cart;
import com.thock.back.market.domain.Order;
import com.thock.back.market.out.repository.CartRepository;
import com.thock.back.market.out.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketCompleteOrderPaymentUseCase {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    /**
     * Payment 모듈로부터 결제 완료 알림을 받아 주문 상태를 업데이트
     * Payment 모듈이 이벤트를 발행하면 MarketEventListener가 이 메서드를 호출함
     * 결제가 완료되었음을 확인하면 주문 시 선택한 상품들 -> 장바구니 상품들을 장바구니에서 제거
     * @param orderId 주문 ID
     */
    @Transactional
    public void completeOrderPayment(String orderId){
        Order order = orderRepository.findByOrderNumber(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // Order 도메인의 completePayment 호출
        order.completePayment();

        // 결제 완료 된 후 장바구니에서 주문했던 상품들을 장바구니에서 제거
        Cart cart = cartRepository.findByBuyer(order.getBuyer())
                        .orElseThrow(() -> new CustomException(ErrorCode.CART_USER_NOT_FOUND));

        if (cart != null) {
            order.getItems().forEach(orderItem -> {
                try {
                    cart.removeItem(orderItem.getProductId());
                } catch (Exception e){
                    log.warn("장바구니 아이템 삭제 실패 (이미 삭제됨): productId={}",
                            orderItem.getProductId());
                }
            });
        }

        log.info("주문 결제 완료 및 장바구니 정리: orderId={}, orderNumber={}",
                orderId, order.getOrderNumber());
    }

}
