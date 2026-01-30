package com.thock.back.market.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Cart;
import com.thock.back.market.domain.CartItem;
import com.thock.back.market.domain.MarketMember;
import com.thock.back.market.domain.Order;
import com.thock.back.market.in.dto.req.OrderCreateRequest;
import com.thock.back.market.in.dto.res.OrderCreateResponse;
import com.thock.back.market.out.api.dto.ProductInfo;
import com.thock.back.market.out.api.dto.WalletInfo;
import com.thock.back.market.out.repository.CartRepository;
import com.thock.back.market.out.repository.MarketMemberRepository;
import com.thock.back.market.out.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketCreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final MarketMemberRepository marketMemberRepository;
    private final CartRepository cartRepository;
    private final MarketSupport marketSupport; // 조회 전용

    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        // 1. 회원 조회
        MarketMember buyer = marketMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_USER_NOT_FOUND));

        // 2. 장바구니 조회
        Cart cart = cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 3. 선택한 CartItem들만 필터링
        List<Long> selectedCartItemIds = request.cartItemIds();

        if (selectedCartItemIds == null || selectedCartItemIds.isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_NO_ITEMS_SELECTED);
        }

        // 선택된 CartItem만 추출
        List<CartItem> selectedCartItems = cart.getItems().stream()
                .filter(item -> selectedCartItemIds.contains(item.getId()))
                .toList();

        // 선택된 상품이 실제로 장바구니에 있는지 확인
        if (selectedCartItems.isEmpty()) {
            throw new CustomException(ErrorCode.CART_EMPTY);
        }

        // 4. 장바구니에 들어있는 상품 정보 조회 - 스냅샷 저장용
        List<Long> productsId = cart.getItems().stream()
                .map(CartItem::getProductId)
                .toList();
        List<ProductInfo> products = marketSupport.getProducts(productsId);

        // ProductInfo를 Map으로 변환 (빠른 조회)
        Map<Long, ProductInfo> productMap = products.stream()
                .collect(Collectors.toMap(ProductInfo::getId, Function.identity()));

        // 5. Order 생성
        Order order = new Order(
                buyer,
                request.zipCode(),
                request.baseAddress(),
                request.detailAddress()
        );

        // 6. OrderItem 추가 (상품 정보 스냅샷, 변경되면 안됨)
        for (CartItem cartItem : cart.getItems()) {
            ProductInfo product = productMap.get(cartItem.getProductId());

            // 상품 정보가 없는 경우 (삭제된 상품 등)
            if (product == null) {
                log.warn("상품 정보를 찾을 수 없음: productId={}", cartItem.getProductId());
                throw new CustomException(ErrorCode.CART_PRODUCT_INFO_NOT_FOUND);
            }

            // 재고 확인
            if (product.getStock() < cartItem.getQuantity()) {
                throw new CustomException(
                        ErrorCode.CART_PRODUCT_OUT_OF_STOCK,
                        String.format("%s 상품의 재고가 부족합니다. (필요: %d개, 재고: %d개)",
                                product.getName(), cartItem.getQuantity(), product.getStock())
                );
            }

            // 주문 아이템 추가 (스냅샷 저장)
            order.addItem(
                    product.getSellerId(),
                    product.getId(),
                    product.getName(),
                    product.getImageUrl(),
                    product.getPrice(),
                    product.getSalePrice(),
                    cartItem.getQuantity()
            );
        }
        // 7. 주문 저장 (OrderItem도 Casecade로 함께 저장됨)
        Order savedOrder = orderRepository.save(order);


        // 8. 예치금 조회 및 pgAmount 계산
        WalletInfo wallet = marketSupport.getWallet(buyer.getId());
        Long balance = wallet.getBalance();

        Long pgAmount = Math.max(0L, savedOrder.getTotalSalePrice() - balance);

        // 9. 결제 요청 (Order 내부에서 pgAmount 계산 후 조건부 이벤트 발행)
        // - pgAmount <= 0: MarketOrderPaymentCompletedEvent 발행 (예치금만)
        // - pgAmount > 0: MarketOrderPaymentRequestedEvent 발행 (PG 필요)
        savedOrder.requestPayment(balance);

        log.info("✅ 주문 생성 완료: orderId={}, orderNumber={}, buyerId={}, totalAmount={}, itemCount={}",
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                buyer.getId(),
                savedOrder.getTotalSalePrice(),
                savedOrder.getItems().size());

        // 11. 응답 생성 및 반환
        return OrderCreateResponse.from(savedOrder, pgAmount);
    }
}
