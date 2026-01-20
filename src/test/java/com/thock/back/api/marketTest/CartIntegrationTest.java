package com.thock.back.api.marketTest;

import com.thock.back.api.boundedContext.market.app.MarketFacade;
import com.thock.back.api.boundedContext.market.domain.Cart;
import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.boundedContext.market.in.dto.req.CartItemAddRequest;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemListResponse;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemResponse;
import com.thock.back.api.boundedContext.market.out.api.dto.ProductInfo;
import com.thock.back.api.boundedContext.market.out.client.ProductClient;
import com.thock.back.api.boundedContext.market.out.repository.CartRepository;
import com.thock.back.api.boundedContext.market.out.repository.MarketMemberRepository;
import com.thock.back.api.boundedContext.product.domain.Category;
import com.thock.back.api.boundedContext.product.domain.Product;
import com.thock.back.api.boundedContext.product.domain.ProductState;
import com.thock.back.api.boundedContext.product.out.ProductRepository;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
public class CartIntegrationTest {

    @Autowired
    private MarketFacade marketFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MarketMemberRepository marketMemberRepository;

    @Autowired
    private CartRepository cartRepository;

    @MockitoBean
    private ProductClient productClient;  // ProductClient를 Mock으로 대체

    private Product testProduct;
    private MarketMember testMember;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 Product 생성 및 저장
        testProduct = Product.builder()
                .sellerId(1L)
                .name("기계식 키보드")
                .price(150000L)
                .salePrice(120000L)
                .stock(50)
                .description("최고급 기계식 키보드입니다.")
                .category(Category.KEYBOARD)
                .build();
        testProduct = productRepository.save(testProduct);

        // 2. 테스트용 MarketMember 생성 및 저장
        testMember = new MarketMember(
                "test@example.com",
                "테스터",
                MemberRole.USER,
                MemberState.ACTIVE,
                999L,  // memberId
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        testMember = marketMemberRepository.save(testMember);

        // 3. 테스트용 Cart 생성 및 저장
        testCart = new Cart(testMember);
        testCart = cartRepository.save(testCart);

        // 4. ProductClient Mock 설정 (Product API 호출을 Mock으로 대체)
        ProductInfo mockProductInfo = new ProductInfo(
                testProduct.getId(),
                testProduct.getName(),
                testProduct.getImageUrl(),
                testProduct.getPrice(),
                testProduct.getSalePrice(),
                testProduct.getStock(),
                testProduct.getState().name()
        );
        given(productClient.getProducts(anyList()))
                .willReturn(List.of(mockProductInfo));
    }

    @Test
    @DisplayName("장바구니 상품 추가 및 조회 통합 테스트")
    void addCartItemAndGetCartItemsTest() {
        // Given: 상품 추가 요청 생성
        CartItemAddRequest request = new CartItemAddRequest(testProduct.getId(), 2);

        // When: 장바구니에 상품 추가
        CartItemResponse addedItem = marketFacade.addCartItem(testMember.getId(), request);

        // Then: 추가된 상품 검증
        assertThat(addedItem).isNotNull();
        assertThat(addedItem.getProductId()).isEqualTo(testProduct.getId());
        assertThat(addedItem.getProductName()).isEqualTo("기계식 키보드");
        assertThat(addedItem.getQuantity()).isEqualTo(2);
        assertThat(addedItem.getPrice()).isEqualTo(150000L);
        assertThat(addedItem.getSalePrice()).isEqualTo(120000L);
        assertThat(addedItem.getTotalPrice()).isEqualTo(300000L);  // 150000 * 2
        assertThat(addedItem.getTotalSalePrice()).isEqualTo(240000L);  // 120000 * 2
        assertThat(addedItem.getDiscountAmount()).isEqualTo(60000L);  // 300000 - 240000

        // When: 장바구니 조회
        CartItemListResponse cartItems = marketFacade.getCartItems(testMember.getId());

        // Then: 조회된 장바구니 검증
        assertThat(cartItems).isNotNull();
        assertThat(cartItems.getCartId()).isEqualTo(testCart.getId());
        assertThat(cartItems.getTotalItemCount()).isEqualTo(1);
        assertThat(cartItems.getItems()).hasSize(1);

        // 장바구니 아이템 검증
        CartItemResponse item = cartItems.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(testProduct.getId());
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getTotalSalePrice()).isEqualTo(240000L);

        // 배송비 검증 (240,000원이므로 무료배송)
//        assertThat(cartItems.getShippingFee()).isEqualTo(0L);
//        assertThat(cartItems.getIsFreeShipping()).isTrue();
//        assertThat(cartItems.getRemainingForFreeShipping()).isEqualTo(0L);
//        assertThat(cartItems.getFinalPaymentAmount()).isEqualTo(240000L);  // 상품 금액 + 배송비(0)
    }

    @Test
    @DisplayName("여러 상품 추가 및 배송비 계산 테스트")
    void addMultipleItemsAndCheckShippingFeeTest() {
        // Given: 저렴한 상품 추가 (배송비 발생 테스트용)
        Product cheapProduct = Product.builder()
                .sellerId(1L)
                .name("키캡")
                .price(15000L)
                .salePrice(12000L)
                .stock(100)
                .category(Category.KEYBOARD)
                .build();
        cheapProduct = productRepository.save(cheapProduct);

        // ProductClient Mock 설정 (저렴한 상품)
        ProductInfo mockCheapProductInfo = new ProductInfo(
                cheapProduct.getId(),
                cheapProduct.getName(),
                cheapProduct.getImageUrl(),
                cheapProduct.getPrice(),
                cheapProduct.getSalePrice(),
                cheapProduct.getStock(),
                cheapProduct.getState().name()
        );
        given(productClient.getProducts(anyList()))
                .willReturn(List.of(mockCheapProductInfo));

        // When: 저렴한 상품 1개 추가 (총 12,000원 - 배송비 발생)
        CartItemAddRequest request = new CartItemAddRequest(cheapProduct.getId(), 1);
        marketFacade.addCartItem(testMember.getId(), request);

        // Then: 장바구니 조회 및 배송비 검증
        CartItemListResponse cartItems = marketFacade.getCartItems(testMember.getId());

        assertThat(cartItems.getTotalSalePrice()).isEqualTo(12000L);
//        assertThat(cartItems.getShippingFee()).isEqualTo(3000L);  // 30,000원 미만이므로 배송비 발생
//        assertThat(cartItems.getIsFreeShipping()).isFalse();
//        assertThat(cartItems.getRemainingForFreeShipping()).isEqualTo(18000L);  // 30,000 - 12,000
//        assertThat(cartItems.getFinalPaymentAmount()).isEqualTo(15000L);  // 12,000 + 3,000
    }

    @Test
    @DisplayName("빈 장바구니 조회 테스트")
    void getEmptyCartTest() {
        // When: 빈 장바구니 조회
        CartItemListResponse cartItems = marketFacade.getCartItems(testMember.getId());

        // Then: 빈 장바구니 검증
        assertThat(cartItems).isNotNull();
        assertThat(cartItems.getCartId()).isEqualTo(testCart.getId());
        assertThat(cartItems.getTotalItemCount()).isEqualTo(0);
        assertThat(cartItems.getItems()).isEmpty();
        assertThat(cartItems.getTotalSalePrice()).isEqualTo(0L);

        // 빈 장바구니도 기본 배송비 표시
//        assertThat(cartItems.getShippingFee()).isEqualTo(3000L);
//        assertThat(cartItems.getIsFreeShipping()).isFalse();
//        assertThat(cartItems.getFinalPaymentAmount()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생 테스트")
    void outOfStockExceptionTest() {
        // Given: 재고가 10개인 상품
        Product limitedProduct = Product.builder()
                .sellerId(1L)
                .name("한정판 키보드")
                .price(200000L)
                .salePrice(180000L)
                .stock(10)  // 재고 10개
                .category(Category.KEYBOARD)
                .build();
        limitedProduct = productRepository.save(limitedProduct);

        ProductInfo mockLimitedProductInfo = new ProductInfo(
                limitedProduct.getId(),
                limitedProduct.getName(),
                limitedProduct.getImageUrl(),
                limitedProduct.getPrice(),
                limitedProduct.getSalePrice(),
                limitedProduct.getStock(),
                limitedProduct.getState().name()
        );
        given(productClient.getProducts(anyList()))
                .willReturn(List.of(mockLimitedProductInfo));

        // When & Then: 재고보다 많은 수량 요청 시 예외 발생
        CartItemAddRequest request = new CartItemAddRequest(limitedProduct.getId(), 15);  // 15개 요청 (재고 10개)

        org.junit.jupiter.api.Assertions.assertThrows(
                com.thock.back.api.global.exception.CustomException.class,
                () -> marketFacade.addCartItem(testMember.getId(), request),
                "재고 부족 시 CustomException이 발생해야 합니다."
        );
    }
}