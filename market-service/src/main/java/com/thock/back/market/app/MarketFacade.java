package com.thock.back.market.app;

import com.thock.back.shared.market.dto.MarketMemberDto;
import com.thock.back.shared.member.dto.MemberDto;
import com.thock.back.market.in.dto.req.CartItemAddRequest;
import com.thock.back.market.in.dto.req.OrderCreateRequest;
import com.thock.back.market.in.dto.res.CartItemListResponse;
import com.thock.back.market.in.dto.res.CartItemResponse;
import com.thock.back.market.in.dto.res.OrderCreateResponse;
import com.thock.back.market.in.dto.res.OrderDetailResponse;
import com.thock.back.shared.market.domain.CancelReasonType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketFacade {

    private final MarketSyncMemberUseCase marketSyncMemberUseCase;
    private final MarketCreateCartUseCase marketCreateCartUseCase;
    // 상태 변경 시나리오
    private final MarketCreateOrderUseCase marketCreateOrderUseCase; // 주문 생성
    private final MarketCompleteOrderPaymentUseCase marketCompleteOrderPaymentUseCase; // 결제 완료
    private final MarketCancelOrderPaymentUseCase marketCancelOrderPaymentUseCase; // 주문 취소
    private final MarketCompleteRefundUseCase marketCompleteRefundUseCase; // 환불
    private final MarketConfirmOrderUseCase marketConfirmOrderUseCase; // 구매 확정
    // 조회 전용
    private final CartService cartService;
    private final OrderService orderService;

    @Transactional
    public void syncMember(MemberDto member) {
        marketSyncMemberUseCase.syncMember(member);
    }

    @Transactional
    public void createCart(MarketMemberDto buyer) {
        marketCreateCartUseCase.createCart(buyer);
    }

    @Transactional(readOnly = true)
    public CartItemListResponse getCartItems(Long memberId){
        return cartService.getCartItems(memberId);
    }

    @Transactional
    public CartItemResponse addCartItem(Long memberId, CartItemAddRequest request){
        return cartService.addCartItem(memberId, request);
    }

    @Transactional
    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        return marketCreateOrderUseCase.createOrder(memberId, request, null);
    }

    @Transactional
    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request, String idempotencyKey) {
        return marketCreateOrderUseCase.createOrder(memberId, request, idempotencyKey);
    }

    @Transactional
    public void completeOrderPayment(String orderNumber){
        marketCompleteOrderPaymentUseCase.completeOrderPayment(orderNumber);
    }

    @Transactional
    public void cancelOrder(Long memberId, Long orderId, CancelReasonType cancelReasonType, String cancelReasonDetail) {
        marketCancelOrderPaymentUseCase.cancelOrder(memberId, orderId, cancelReasonType, cancelReasonDetail);
    }

    @Transactional
    public void cancelOrderItems(Long memberId, Long orderId, List<Long> orderItemIds, CancelReasonType cancelReasonType, String cancelReasonDetail) {
        marketCancelOrderPaymentUseCase.cancelOrderItems(memberId, orderId, orderItemIds, cancelReasonType, cancelReasonDetail);
    }


    @Transactional
    public void clearCart(Long memberId) {
        cartService.clearCart(memberId);
    }

    @Transactional
    public void removeCartItems(Long memberId, List<Long> productIds) {
        cartService.removeCartItems(memberId, productIds);
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getMyOrders(Long memberId) {
        return orderService.getMyOrders(memberId);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long memberId, Long orderId) {
        return orderService.getOrderDetail(memberId, orderId);
    }

    @Transactional
    public void completeRefund(String orderNumber) {
        marketCompleteRefundUseCase.completeRefund(orderNumber);
    }

    @Transactional
    public void confirmOrder(Long memberId, Long orderId) {
        marketConfirmOrderUseCase.confirmOrder(memberId, orderId);
    }

    @Transactional
    public void confirmOrderItems(Long memberId, Long orderId, List<Long> orderItemIds) {
        marketConfirmOrderUseCase.confirmOrderItems(memberId, orderId, orderItemIds);
    }
}
