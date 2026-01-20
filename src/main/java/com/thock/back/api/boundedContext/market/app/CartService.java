package com.thock.back.api.boundedContext.market.app;

import com.thock.back.api.boundedContext.market.domain.Cart;
import com.thock.back.api.boundedContext.market.domain.CartItem;
import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.boundedContext.market.in.dto.req.CartItemAddRequest;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemListResponse;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemResponse;
import com.thock.back.api.boundedContext.market.out.api.dto.ProductInfo;
import com.thock.back.api.boundedContext.market.out.repository.CartRepository;
import com.thock.back.api.boundedContext.market.out.repository.MarketMemberRepository;
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

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final MarketSupport marketSupport;
    private final CartRepository cartRepository;
    private final MarketMemberRepository marketMemberRepository;

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

    /**
     * 장바구니에 상품 추가
     * @param memberId 회원 ID
     * @param request 상품 추가 요청 (productId, quantity)
     * @return 추가된 상품 정보
     */
    @Transactional
    public CartItemResponse addCartItem(Long memberId, CartItemAddRequest request) {
        MarketMember member = marketMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_USER_NOT_FOUND));

        Cart cart = cartRepository.findByBuyer(member)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        // Product 정보 조회 - API Call
        ProductInfo product = marketSupport.getProduct(request.getProductId());
        if (product == null) {
            throw new CustomException(ErrorCode.CART_PRODUCT_API_FAILED);
        }

        // 재고 확인
        if (product.getStock() < request.getQuantity()) {
             throw new CustomException(ErrorCode.CART_PRODUCT_OUT_OF_STOCK);
        }

        // TODO : 영속성 상태에 대해서 제대로 공부가 필요함.
        CartItem addedCartItem = cart.addItem(request.getProductId(), request.getQuantity());

        // ✅ 로그 추가: 생성 직후 값 확인
        log.info("🔍 CartItem 생성 직후: id={}, productId={}, quantity={}, cartId={}",
                addedCartItem.getId(),
                addedCartItem.getProductId(),
                addedCartItem.getQuantity(),
                addedCartItem.getCart() != null ? addedCartItem.getCart().getId() : "null"
        );

        // ⭐ 명시적으로 저장 - Cascade.PERSIST로 CartItem도 함께 영속화
//        cartRepository.save(cart);
//        cartRepository.flush();

        // ✅ 로그 추가: flush 직후 값 확인
        log.info("💾 flush 직후: id={}, productId={}, quantity={}, cartId={}",
                addedCartItem.getId(),
                addedCartItem.getProductId(),
                addedCartItem.getQuantity(),
                addedCartItem.getCart() != null ? addedCartItem.getCart().getId() : "null"
        );

        // CartItemResponse 생성 및 반환
        Long totalPrice = addedCartItem.getQuantity() * product.getPrice();
        Long totalSalePrice = addedCartItem.getQuantity() * product.getSalePrice();
        Long discountAmount = totalPrice - totalSalePrice;

        return new CartItemResponse(
                addedCartItem.getId(),
                addedCartItem.getQuantity(),
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
    }
}
