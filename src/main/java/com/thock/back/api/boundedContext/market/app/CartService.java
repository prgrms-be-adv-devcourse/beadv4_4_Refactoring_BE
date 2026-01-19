package com.thock.back.api.boundedContext.market.app;

import com.thock.back.api.boundedContext.market.domain.Cart;
import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemListResponse;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemResponse;
import com.thock.back.api.boundedContext.market.out.api.dto.ProductInfo;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final MarketSupport marketSupport;

    // 장바구니 조회
    public CartItemListResponse getCartItems(Long memberId){

        // MarketMember가 존재하는지 확인
        MarketMember member = marketSupport.findMemberById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_USER_NOT_FOUND));

        // 장바구니
        Cart cart = marketSupport.findCartByBuyer(member)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 장바구니가 비어있으면 빈 응답 반환
        if (!cart.hasItems()) {
            return new CartItemListResponse(cart.getId(), List.of(), 0, 0L, 0L, 0L);
        }

        // Product 정보와 함께 CartItemResponse 생성
        List<CartItemResponse> items = cart.getItems().stream()
                .map(cartItem -> {
                    // MarketSupport를 통해 Product 정보 조회
                    ProductInfo product = marketSupport.getProduct(cartItem.getProductId());

                    if (product == null) {
                        return null; // Product 정보 없으면 제외
                    }

                    // 계산된 값
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
                .filter(item -> item != null)
                .collect(Collectors.toList());

        // 전체 합계 계산
        Integer totalItemCount = items.size();
        Long totalPrice = items.stream().mapToLong(CartItemResponse::getTotalPrice).sum();
        Long totalSalePrice = items.stream().mapToLong(CartItemResponse::getTotalSalePrice).sum();
        Long totalDiscountAmount = totalPrice - totalSalePrice;

        return new CartItemListResponse(
                cart.getId(),
                items,
                totalItemCount,
                totalPrice,
                totalSalePrice,
                totalDiscountAmount
        );
    }

}
