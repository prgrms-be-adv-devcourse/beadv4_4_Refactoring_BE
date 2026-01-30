package com.thock.back.api.boundedContext.market.app;

import com.thock.back.api.boundedContext.market.domain.Cart;
import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.boundedContext.market.in.dto.req.CartItemAddRequest;
import com.thock.back.api.boundedContext.market.in.dto.req.OrderCreateRequest;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemListResponse;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemResponse;
import com.thock.back.api.boundedContext.market.in.dto.res.OrderCreateResponse;
import com.thock.back.api.boundedContext.market.in.dto.res.OrderDetailResponse;
import com.thock.back.api.shared.market.dto.MarketMemberDto;
import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketFacade {

    private final MarketSyncMemberUseCase marketSyncMemberUseCase;
    private final MarketCreateCartUseCase marketCreateCartUseCase;
    private final MarketCreateOrderUseCase marketCreateOrderUseCase;
    private final MarketCompleteOrderPaymentUseCase marketCompleteOrderPaymentUseCase;
    private final MarketCancelOrderPaymentUseCase marketCancelOrderPaymentUseCase;
    private final CartService cartService;
    private final OrderService orderService;

    @Transactional
    public MarketMember syncMember(MemberDto member) {
        return marketSyncMemberUseCase.syncMember(member);
    }

    @Transactional
    public Cart createCart(MarketMemberDto buyer) {
        return marketCreateCartUseCase.createCart(buyer);
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
        return marketCreateOrderUseCase.createOrder(memberId, request);
    }

    @Transactional
    public void completeOrderPayment(String orderId){
        marketCompleteOrderPaymentUseCase.completeOrderPayment(orderId);
    }

    @Transactional
    public void cancelOrder(Long memberId, Long orderId) {
        marketCancelOrderPaymentUseCase.cancelOrder(memberId, orderId);
    }

    @Transactional
    public void cancelOrderItem(Long memberId, Long orderId, Long orderItemId) {
        marketCancelOrderPaymentUseCase.cancelOrderItem(memberId, orderId, orderItemId);
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

}
