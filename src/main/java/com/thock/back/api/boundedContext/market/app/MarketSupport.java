package com.thock.back.api.boundedContext.market.app;

import com.thock.back.api.boundedContext.market.domain.Cart;
import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.boundedContext.market.out.api.dto.ProductInfo;
import com.thock.back.api.boundedContext.market.out.client.ProductClient;
import com.thock.back.api.boundedContext.market.out.repository.CartRepository;
import com.thock.back.api.boundedContext.market.out.repository.MarketMemberRepository;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketSupport {
    private final MarketMemberRepository marketMemberRepository;
    private final CartRepository cartRepository;
    /**
     * 나중에 RestClient 대신 Feign, WebClient 사용하고 싶으면?
     * 인터페이스 방식: 구현체만 바꾸면 됨 → MarketSupport 코드 수정 불필요
     * 구체 클래스 방식: MarketSupport 코드도 수정해야 함
     */
    private final ProductClient productClient; // 인터페이스로 주입 : ProductApiClient 라는 구체 클래스에 의존❌

    public Optional<Cart> findCartByBuyer(MarketMember buyer) {
        return cartRepository.findByBuyer(buyer);
    }

    public Optional<MarketMember> findMemberById(Long id) {
        return marketMemberRepository.findById(id);
    }

    /**
     * Product 정보 조회
     * @param productId 상품 ID
     * @return Product 정보 (실패 시 null)
     */
    public ProductInfo getProduct(Long productId) {
        try {
            ProductInfo product = productClient.getProduct(productId);
            if (product == null) {
                log.warn("Product 정보가 null: productId={}", productId);
                return null;  // null은 허용 (상품이 삭제된 경우 등)
            }
            return product;

        } catch (Exception e) {
            log.error("Product API 호출 실패: productId={}", productId, e);
            // Product API 자체가 다운된 경우는 예외 발생
            throw new CustomException(ErrorCode.CART_PRODUCT_API_FAILED, e);
        }
    }
}
