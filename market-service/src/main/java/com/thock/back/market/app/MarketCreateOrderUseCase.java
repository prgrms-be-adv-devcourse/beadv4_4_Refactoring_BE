package com.thock.back.market.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Cart;
import com.thock.back.market.domain.CartItem;
import com.thock.back.market.domain.MarketMember;
import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderState;
import com.thock.back.market.in.dto.req.OrderCreateRequest;
import com.thock.back.market.in.dto.res.OrderCreateResponse;
import com.thock.back.market.out.api.dto.ProductInfo;
import com.thock.back.market.out.api.dto.WalletInfo;
import com.thock.back.market.out.repository.CartRepository;
import com.thock.back.market.out.repository.MarketMemberRepository;
import com.thock.back.market.out.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 주문 생성 - 장바구니 내 선택한 상품들만 주문에 들어감
    @Transactional
    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        return createOrder(memberId, request, null);
    }

    @Transactional
    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request, String idempotencyKey) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        MarketMember buyer = getBuyerForUpdate(memberId);

        OrderCreateResponse idempotentResponse = findExistingOrderByIdempotencyKey(memberId, normalizedIdempotencyKey);
        if (idempotentResponse != null) {
            return idempotentResponse;
        }

        validateNoPendingOrder(memberId);

        Cart cart = getCartByBuyer(buyer);
        List<CartItem> selectedCartItems = getSelectedCartItems(cart, request.cartItemIds());
        Map<Long, ProductInfo> productMap = getProductMap(selectedCartItems);

        Order order = createOrderAggregate(buyer, request, normalizedIdempotencyKey);
        appendOrderItems(order, selectedCartItems, productMap);

        Order savedOrder;

        try {
            savedOrder = orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException e) {
            if (normalizedIdempotencyKey == null) {
                throw e;
            }

            log.info("멱등 키 유니크 충돌 감지: memberId={}, idempotencyKey={}",
                    memberId, normalizedIdempotencyKey);

            return orderRepository.findByBuyerIdAndIdempotencyKey(memberId,
                            normalizedIdempotencyKey)
                    .map(existingOrder -> {
                        WalletInfo wallet = marketSupport.getWallet(buyer.getId());
                        Long balance = wallet.getBalance();
                        Long pgAmount = Math.max(0L, existingOrder.getTotalSalePrice() -
                                balance);
                        return OrderCreateResponse.from(existingOrder, pgAmount);
                    })
                    .orElseThrow(() -> e);
        }



        WalletInfo wallet = marketSupport.getWallet(buyer.getId());
        Long balance = wallet.getBalance();
        Long pgAmount = Math.max(0L, savedOrder.getTotalSalePrice() - balance);

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

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private MarketMember getBuyerForUpdate(Long memberId) {
        return marketMemberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_USER_NOT_FOUND));
    }

    private OrderCreateResponse findExistingOrderByIdempotencyKey(Long memberId, String normalizedIdempotencyKey) {
        if (normalizedIdempotencyKey == null) {
            return null;
        }

        return orderRepository.findByBuyerIdAndIdempotencyKey(memberId, normalizedIdempotencyKey)
                .map(order -> {
                    log.info("멱등 키 재요청 감지: memberId={}, orderId={}, orderNumber={}",
                            memberId, order.getId(), order.getOrderNumber());
                    WalletInfo wallet = marketSupport.getWallet(memberId);
                    Long balance = wallet.getBalance();
                    Long pgAmount = Math.max(0L, order.getTotalSalePrice() - balance);
                    return OrderCreateResponse.from(order, pgAmount);
                })
                .orElse(null);
    }

    private void validateNoPendingOrder(Long memberId) {
        if (orderRepository.existsByBuyerIdAndState(memberId, OrderState.PENDING_PAYMENT)) {
            throw new CustomException(ErrorCode.ORDER_PENDING_EXISTS);
        }
    }

    private Cart getCartByBuyer(MarketMember buyer) {
        return cartRepository.findByBuyer(buyer)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    private List<CartItem> getSelectedCartItems(Cart cart, List<Long> selectedCartItemIds) {
        if (selectedCartItemIds == null || selectedCartItemIds.isEmpty()) {
            throw new CustomException(ErrorCode.ORDER_NO_ITEMS_SELECTED);
        }

        List<CartItem> selectedCartItems = cart.getItems().stream()
                .filter(item -> selectedCartItemIds.contains(item.getId()))
                .toList();

        if (selectedCartItems.isEmpty()) {
            throw new CustomException(ErrorCode.CART_EMPTY);
        }
        return selectedCartItems;
    }

    private Map<Long, ProductInfo> getProductMap(List<CartItem> selectedCartItems) {
        List<Long> productsId = selectedCartItems.stream()
                .map(CartItem::getProductId)
                .toList();

        return marketSupport.getProducts(productsId).stream()
                .collect(Collectors.toMap(ProductInfo::getId, Function.identity()));
    }

    private Order createOrderAggregate(MarketMember buyer, OrderCreateRequest request, String normalizedIdempotencyKey) {
        Order order = new Order(
                buyer,
                request.zipCode(),
                request.baseAddress(),
                request.detailAddress()
        );
        order.assignIdempotencyKey(normalizedIdempotencyKey);
        return order;
    }

    private void appendOrderItems(Order order, List<CartItem> selectedCartItems, Map<Long, ProductInfo> productMap) {
        for (CartItem cartItem : selectedCartItems) {
            ProductInfo product = productMap.get(cartItem.getProductId());

            if (product == null) {
                log.warn("상품 정보를 찾을 수 없음: productId={}", cartItem.getProductId());
                throw new CustomException(ErrorCode.CART_PRODUCT_INFO_NOT_FOUND);
            }

            if (product.getStock() < cartItem.getQuantity()) {
                throw new CustomException(
                        ErrorCode.CART_PRODUCT_OUT_OF_STOCK,
                        String.format("%s 상품의 재고가 부족합니다. (필요: %d개, 재고: %d개)",
                                product.getName(), cartItem.getQuantity(), product.getStock())
                );
            }

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
    }
}
