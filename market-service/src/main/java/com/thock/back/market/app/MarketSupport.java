package com.thock.back.market.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.market.domain.Cart;
import com.thock.back.market.domain.MarketMember;
import com.thock.back.market.out.api.dto.ProductInfo;
import com.thock.back.market.out.api.dto.WalletInfo;
import com.thock.back.market.out.client.PaymentWalletClient;
import com.thock.back.market.out.client.ProductClient;
import com.thock.back.market.out.repository.CartRepository;
import com.thock.back.market.out.repository.MarketMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketSupport {
    private final MarketMemberRepository marketMemberRepository;
    private final CartRepository cartRepository;
    /**
     * 나중에 RestClient 대신 Feign, WebClient 사용하고 싶으면?
     * 인터페이스 방식: 구현체만 바꾸면 됨 → MarketSupport 코드 수정 불필요 -> FeignClient로 변경 완료
     * 구체 클래스 방식: MarketSupport 코드도 수정해야 함
     */
    private final ProductClient productClient; // 인터페이스로 주입 : ProductApiClient 라는 구체 클래스에 의존❌
    private final PaymentWalletClient paymentWalletClient;

    public Optional<Cart> findCartByBuyer(MarketMember buyer) {
        return cartRepository.findByBuyer(buyer);
    }

    public Optional<MarketMember> findMemberById(Long id) {
        return marketMemberRepository.findById(id);
    }




    // Product 정보 조회 - 단건
    @Transactional(readOnly = true)
    public ProductInfo getProduct(Long productId) {
        List<ProductInfo> products = productClient.getProducts(List.of(productId));

        if (products == null || products.isEmpty()) {
            log.warn("Product 정보가 없음: productId={}", productId);
            throw new CustomException(ErrorCode.CART_PRODUCT_INFO_NOT_FOUND);
        }

        return products.get(0);
    }

    // Cart에 들어있는 여러 CartItem 조회
    @Transactional(readOnly = true)
    public List<ProductInfo> getProducts(List<Long> productIds) {

        List<ProductInfo> products = productClient.getProducts(productIds);

        if (products == null) {
            log.warn("Product 정보 리스트가 null: productIds={}", productIds);
            throw new CustomException(ErrorCode.CART_PRODUCT_INFO_NOT_FOUND);
        }

        return products;
    }

    @Transactional(readOnly = true)
    public WalletInfo getWallet(Long memberId) {
        WalletInfo wallet = paymentWalletClient.getWallet(memberId);

        if (wallet == null) {
            log.warn("Wallet 정보가 null: memberId={}", memberId);
            throw new CustomException(ErrorCode.WALLET_NOT_FOUND);
        }

        return wallet;
    }
}
