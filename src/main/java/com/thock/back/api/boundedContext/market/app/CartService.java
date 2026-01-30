package com.thock.back.api.boundedContext.market.app;

import com.thock.back.api.boundedContext.market.domain.Cart;
import com.thock.back.api.boundedContext.market.domain.CartItem;
import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.boundedContext.market.in.dto.req.CartItemAddRequest;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemListResponse;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemResponse;
import com.thock.back.api.boundedContext.market.out.api.dto.ProductInfo;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final MarketSupport marketSupport;

    // 장바구니 조회
    @Transactional(readOnly = true)
    public CartItemListResponse getCartItems(Long memberId){

        // MarketMember가 존재하는지 확인
        MarketMember member = marketSupport.findMemberById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_USER_NOT_FOUND));

        // 장바구니
        Cart cart = marketSupport.findCartByBuyer(member)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        // 장바구니가 비어있으면 빈 응답 반환
        if (!cart.hasItems()) {
            return new CartItemListResponse(cart.getId(), List.of(), 0, 0L, 0L, 0L);
        }

        // productId 리스트 추출
        List<Long> productIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .toList();

        // 여러 개 조회 : getProducts() 사용
        List<ProductInfo> products = marketSupport.getProducts(productIds);

        // Map으로 변환
        Map<Long, ProductInfo> productMap = products.stream()
                .collect(Collectors.toMap(ProductInfo::getId, Function.identity()));

        // CartItemResponse 생성
        List<CartItemResponse> items = cart.getItems().stream()
                .map(cartItem -> {
                    ProductInfo product = productMap.get(cartItem.getProductId());

                    if (product == null) {
                        log.warn("장바구니에 있지만 상품 정보가 없음: productId={}", cartItem.getProductId());
                        return null;
                    }

                    Long totalPrice = cartItem.getQuantity() * product.getPrice();
                    Long totalSalePrice = cartItem.getQuantity() * product.getSalePrice();
                    Long discountAmount = totalPrice - totalSalePrice;

                    return new CartItemResponse(
                            cartItem.getId(),
                            cartItem.getQuantity(),
                            product.getId(),
                            product.getName(),
                            product.getImageUrl(),
                            product.getPrice(),
                            product.getSalePrice(),
                            product.getStock(),
                            totalPrice,
                            totalSalePrice,
                            discountAmount
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 전체 합계 계산 : 계산을 req의 정적 팩토리 메서드가 담당함
//        Integer totalItemCount = items.size();
//        Long totalPrice = items.stream().mapToLong(CartItemResponse::totalPrice).sum();
//        Long totalSalePrice = items.stream().mapToLong(CartItemResponse::totalSalePrice).sum();
//        Long totalDiscountAmount = totalPrice - totalSalePrice;

        return CartItemListResponse.from(cart, items);
    }

    /**
     * 장바구니에 상품 추가
     * @param memberId 회원 ID
     * @param request 상품 추가 요청 (productId, quantity)
     * @return 추가된 상품 정보
     */
    @Transactional
    public CartItemResponse addCartItem(Long memberId, CartItemAddRequest request) {

        MarketMember member = marketSupport.findMemberById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_USER_NOT_FOUND));
        Cart cart = marketSupport.findCartByBuyer(member)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        // Product 정보 조회 - API Call
        ProductInfo product = marketSupport.getProduct(request.productId());
        if (product == null) {
            throw new CustomException(ErrorCode.CART_PRODUCT_API_FAILED);
        }

        // 재고 확인
        if (product.getStock() < request.quantity()) {
             throw new CustomException(ErrorCode.CART_PRODUCT_OUT_OF_STOCK);
        }

        CartItem addedCartItem = cart.addItem(request.productId(), request.quantity());


        return CartItemResponse.from(addedCartItem, product);
    }

    @Transactional
    public void clearCart(Long memberId) {
        MarketMember member = marketSupport.findMemberById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_USER_NOT_FOUND));

        Cart cart = marketSupport.findCartByBuyer(member)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        cart.clearItems();
    }

    @Transactional
    public void removeCartItems(Long memberId, List<Long> productIds) {
        MarketMember member = marketSupport.findMemberById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_USER_NOT_FOUND));

        Cart cart = marketSupport.findCartByBuyer(member)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        for (Long productId : productIds) {
            cart.removeItem(productId);
        }
    }
}
