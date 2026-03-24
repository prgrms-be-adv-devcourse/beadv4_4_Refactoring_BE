package com.thock.back.product.messaging.inbox;

import com.thock.back.product.ProductServiceApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = {
                ProductServiceApplication.class,
                ProductInboxDatabaseIntegrationTest.TestConfig.class
        },
        properties = {
                "product.inbox.enabled=true",
                "spring.kafka.listener.auto-startup=false"
        }
)
@ActiveProfiles("test")
class ProductInboxDatabaseIntegrationTest {

    private static final String TOPIC = "market.order.stock.changed";
    private static final String CONSUMER_GROUP = "product-service";

    @Autowired
    private ProductInboxGuard inboxGuard;

    @Autowired
    private ProductInboxEventRepository inboxEventRepository;

    @Autowired
    private ClaimAndFailService claimAndFailService;

    @AfterEach
    void tearDown() {
        inboxEventRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("claimIfAbsent inserts only once for the same key and consumer group")
    void tryClaim_whenDuplicateKeyExists_onlyFirstInsertSucceeds() {
        boolean firstClaimed = inboxGuard.tryClaim("order-1:reserve:1-2", TOPIC, CONSUMER_GROUP);
        boolean secondClaimed = inboxGuard.tryClaim("order-1:reserve:1-2", TOPIC, CONSUMER_GROUP);

        assertThat(firstClaimed).isTrue();
        assertThat(secondClaimed).isFalse();
        assertThat(inboxEventRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("claimIfAbsent allows the same key for a different consumer group")
    void tryClaim_whenConsumerGroupDiffers_allowsAnotherInsert() {
        boolean firstClaimed = inboxGuard.tryClaim("order-2:reserve:1-2", TOPIC, CONSUMER_GROUP);
        boolean secondClaimed = inboxGuard.tryClaim("order-2:reserve:1-2", TOPIC, "settlement-service");

        assertThat(firstClaimed).isTrue();
        assertThat(secondClaimed).isTrue();
        assertThat(inboxEventRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("claim is rolled back when the outer transaction fails")
    void tryClaim_whenOuterTransactionRollsBack_canClaimAgain() {
        assertThatThrownBy(() ->
                claimAndFailService.claimThenFail("order-3:reserve:1-2", TOPIC, CONSUMER_GROUP)
        ).isInstanceOf(ForcedRollbackException.class);

        boolean claimedAfterRollback = inboxGuard.tryClaim("order-3:reserve:1-2", TOPIC, CONSUMER_GROUP);

        assertThat(claimedAfterRollback).isTrue();
        assertThat(inboxEventRepository.count()).isEqualTo(1);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ClaimAndFailService claimAndFailService(ProductInboxGuard inboxGuard) {
            return new ClaimAndFailService(inboxGuard);
        }
    }

    static class ClaimAndFailService {

        private final ProductInboxGuard inboxGuard;

        ClaimAndFailService(ProductInboxGuard inboxGuard) {
            this.inboxGuard = inboxGuard;
        }

        @Transactional
        public void claimThenFail(String idempotencyKey, String topic, String consumerGroup) {
            boolean claimed = inboxGuard.tryClaim(idempotencyKey, topic, consumerGroup);
            if (!claimed) {
                throw new IllegalStateException("Claim should succeed before rollback");
            }
            throw new ForcedRollbackException();
        }
    }

    static class ForcedRollbackException extends RuntimeException {
    }
}
