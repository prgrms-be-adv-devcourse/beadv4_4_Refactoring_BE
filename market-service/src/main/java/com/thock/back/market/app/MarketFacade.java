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
import org.springframework.dao.DataIntegrityViolationException;
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

    // 주문 생성 로직 (트랜잭션 분리: 멱등성 키 충돌(DataIntegrityViolationException) 시
    // 예외를 잡아서 복구하기 위해 Facade 계층에서는 트랜잭션을 열지 않음)
    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request, String idempotencyKey) {
        // 1. 트랜잭션 시작 전, 멱등성 키로 기존 주문 확인
        OrderCreateResponse existingOrder = marketCreateOrderUseCase.findExistingOrderByIdempotencyKey(memberId, idempotencyKey);
        // 순차 요청에 대한 중복 주문 생성 방지 - ex) 3초 뒤에 다시 버튼 클릭
        if (existingOrder != null) {
            return existingOrder; // 기존 주문을 반환
        }

        try {
            // 2. 실제 주문 생성 (이 내부에서 @Transactional이 동작하여 새로운 트랜잭션 시작/종료)
            return marketCreateOrderUseCase.createOrder(memberId, request, idempotencyKey);
        }
        // 동시 요청에 대한 중복 주문 생성 방지 - ex) 순간적으로 2번 클릭 or 여러 브라우저 띄워놓고 동시에 요청
        catch (DataIntegrityViolationException e) {
            // 3. DB 유니크 제약조건(멱등성 키 중복) 충돌 발생 시
            // UseCase의 트랜잭션은 롤백되었지만, Facade는 트랜잭션 밖이므로 UnexpectedRollbackException 터질 일이 없음
            OrderCreateResponse retryExistingOrder = marketCreateOrderUseCase.findExistingOrderByIdempotencyKey(memberId, idempotencyKey);

            if (retryExistingOrder != null) {
                return retryExistingOrder;
            }
            // 정말 알 수 없는 무결성 위반이라면 예외 다시 던지기
            throw e;
        }
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
