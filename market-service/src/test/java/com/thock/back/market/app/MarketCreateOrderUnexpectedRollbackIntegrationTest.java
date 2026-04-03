package com.thock.back.market.app;

import com.thock.back.market.domain.Cart;
import com.thock.back.market.domain.MarketMember;
import com.thock.back.market.domain.MarketPolicy;
import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderState;
import com.thock.back.market.in.dto.req.OrderCreateRequest;
import com.thock.back.market.in.dto.res.OrderCreateResponse;
import com.thock.back.market.out.api.dto.ProductInfo;
import com.thock.back.market.out.api.dto.WalletInfo;
import com.thock.back.market.out.client.PaymentWalletClient;
import com.thock.back.market.out.client.ProductClient;
import com.thock.back.market.out.repository.CartRepository;
import com.thock.back.market.out.repository.MarketMemberRepository;
import com.thock.back.market.out.repository.OrderRepository;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:market-it;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
class MarketCreateOrderUnexpectedRollbackIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(MarketCreateOrderUnexpectedRollbackIntegrationTest.class);

    @Autowired
    private MarketFacade marketFacade;

    @Autowired
    private MarketMemberRepository marketMemberRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private OrderRepository spyOrderRepository;

    @MockitoBean
    private ProductClient productClient;

    @MockitoBean
    private PaymentWalletClient paymentWalletClient;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        marketMemberRepository.deleteAll();
    }

    @Test
    @DisplayName("동시 요청으로 unique 충돌이 발생해도 UnexpectedRollbackException 없이 기존 주문을 반환한다")
    void createOrder_uniqueConflict_doesNotThrowUnexpectedRollbackException_andReturnsExistingOrder() throws Exception {
        Long memberId = 1L;
        String idempotencyKey = "idem-key-1";

        MarketMember buyer = new MarketMember(
                "buyer@test.com",
                "buyer",
                MemberRole.USER,
                MemberState.ACTIVE,
                memberId,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        marketMemberRepository.saveAndFlush(buyer);

        jdbcTemplate.update(
                "insert into market_carts (id, buyer_id, items_count, created_at, updated_at) values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                memberId,
                memberId,
                1
        );
        jdbcTemplate.update(
                "insert into market_cart_items (cart_id, product_id, quantity, created_at, updated_at) values (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                memberId,
                10L,
                1
        );

        Long cartItemId = jdbcTemplate.queryForObject(
                "select id from market_cart_items where cart_id = ?",
                Long.class,
                memberId
        );

        Order existingOrder = new Order(buyer, "12345", "서울시 강남구", "101호");
        existingOrder.assignIdempotencyKey(idempotencyKey);
        ReflectionTestUtils.setField(existingOrder, "state", OrderState.CANCELLED);
        orderRepository.saveAndFlush(existingOrder);

        MarketPolicy.PRODUCT_PAYOUT_RATE = 90.0;

        given(productClient.getProducts(anyList())).willReturn(List.of(
                new ProductInfo(
                        10L,
                        2L,
                        "기계식 키보드",
                        "keyboard.jpg",
                        50_000L,
                        40_000L,
                        10,
                        "ON_SALE"
                )
        ));
        given(paymentWalletClient.getWallet(eq(memberId))).willReturn(new WalletInfo(0L));

        AtomicInteger lookupCount = new AtomicInteger();
        doAnswer(invocation -> {
            Long buyerArg = invocation.getArgument(0);
            String keyArg = invocation.getArgument(1);

            if (lookupCount.getAndIncrement() == 0) {
                return Optional.empty();
            }
            Long orderId = jdbcTemplate.queryForObject(
                    "select id from market_orders where buyer_id = ? and idempotency_key = ?",
                    Long.class,
                    buyerArg,
                    keyArg
            );
            return orderRepository.findById(orderId);
        }).when(spyOrderRepository).findByBuyerIdAndIdempotencyKey(memberId, idempotencyKey);

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(cartItemId),
                "12345",
                "서울시 강남구",
                "101호"
        );

        // 과거에는 같은 트랜잭션 내부에서 예외를 복구하려다 UnexpectedRollbackException이 발생했지만,
        // 현재는 Facade가 비트랜잭션으로 동작하므로 기존 주문 응답으로 정상 복구되어야 한다.
        OrderCreateResponse response = marketFacade.createOrder(memberId, request, idempotencyKey);

        assertThat(response.orderNumber()).isEqualTo(existingOrder.getOrderNumber());
        assertThat(response.pgAmount()).isEqualTo(existingOrder.getTotalSalePrice());
        log.info("Recovered existing order after unique conflict: orderNumber={}", response.orderNumber());

    }
}
