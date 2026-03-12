package com.thock.back.product.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.product.ProductServiceApplication;
import com.thock.back.product.app.ProductCreateService;
import com.thock.back.product.app.ProductManageService;
import com.thock.back.product.domain.Category;
import com.thock.back.product.domain.command.ProductCreateCommand;
import com.thock.back.product.domain.command.ProductUpdateCommand;
import com.thock.back.product.domain.entity.Product;
import com.thock.back.product.out.ProductRepository;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.product.event.ProductEvent;
import com.thock.back.shared.product.event.ProductEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ProductServiceApplication.class,
        properties = {
                "product.outbox.enabled=true",
                "product.inbox.enabled=false",
                "spring.kafka.listener.auto-startup=false"
        }
)
@ActiveProfiles("test")
class ProductOutboxEventPublisherIntegrationTest {

    @Autowired
    private ProductCreateService productCreateService;

    @Autowired
    private ProductManageService productManageService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOutboxEventRepository productOutboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Scheduled poller가 테스트 도중 row 상태를 바꾸지 않도록 mock으로 교체한다.
    @MockitoBean
    private ProductOutboxPoller productOutboxPoller;

    @AfterEach
    void tearDown() {
        productOutboxEventRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    // ProductCreateService.createProduct() 호출 후 product_outbox_event에 PENDING row가 저장되는지 검증 topic, eventType, eventKey, payload 내용까지 확인
    @Test
    @DisplayName("createProduct stores a pending product.changed event in the outbox")
    void createProduct_whenSuccessful_savesPendingOutboxEvent() throws Exception {
        ProductCreateCommand command = new ProductCreateCommand(
                101L,
                MemberRole.SELLER,
                "Keychron Q1",
                230_000L,
                210_000L,
                15,
                Category.KEYBOARD,
                "mechanical keyboard",
                "https://image.example/q1.png",
                Map.of("switch", "red")
        );

        Long savedProductId = productCreateService.createProduct(command);

        List<ProductOutboxEvent> outboxEvents = productOutboxEventRepository.findAll();

        assertThat(productRepository.existsById(savedProductId)).isTrue();
        assertThat(outboxEvents).hasSize(1);

        ProductOutboxEvent outboxEvent = outboxEvents.get(0);
        ProductEvent payload = objectMapper.readValue(outboxEvent.getPayload(), ProductEvent.class);

        assertThat(outboxEvent.getTopic()).isEqualTo(KafkaTopics.PRODUCT_CHANGED);
        assertThat(outboxEvent.getEventType()).isEqualTo(ProductEvent.class.getName());
        assertThat(outboxEvent.getEventKey()).isEqualTo(String.valueOf(savedProductId));
        assertThat(outboxEvent.getStatus()).isEqualTo(ProductOutboxStatus.PENDING);
        assertThat(outboxEvent.getCreatedAt()).isNotNull();

        assertThat(payload.productId()).isEqualTo(savedProductId);
        assertThat(payload.sellerId()).isEqualTo(101L);
        assertThat(payload.eventType()).isEqualTo(ProductEventType.CREATE);
    }

    @Test
    @DisplayName("updateProduct stores a pending product.changed update event in the outbox")
    void updateProduct_whenSuccessful_savesPendingOutboxEvent() throws Exception {
        Product savedProduct = productRepository.save(Product.builder()
                .sellerId(201L)
                .name("Keychron V1")
                .price(180_000L)
                .salePrice(170_000L)
                .stock(20)
                .category(Category.KEYBOARD)
                .description("before update")
                .imageUrl("https://image.example/v1-before.png")
                .detail(Map.of("switch", "brown"))
                .build());

        ProductUpdateCommand command = new ProductUpdateCommand(
                savedProduct.getId(),
                201L,
                MemberRole.SELLER,
                "Keychron V1 Max",
                190_000L,
                175_000L,
                18,
                Category.KEYBOARD,
                "after update",
                "https://image.example/v1-after.png",
                Map.of("switch", "banana")
        );

        Long updatedProductId = productManageService.updateProduct(command);

        List<ProductOutboxEvent> outboxEvents = productOutboxEventRepository.findAll();

        assertThat(updatedProductId).isEqualTo(savedProduct.getId());
        assertThat(outboxEvents).hasSize(1);

        ProductOutboxEvent outboxEvent = outboxEvents.get(0);
        ProductEvent payload = objectMapper.readValue(outboxEvent.getPayload(), ProductEvent.class);

        assertThat(outboxEvent.getTopic()).isEqualTo(KafkaTopics.PRODUCT_CHANGED);
        assertThat(outboxEvent.getEventType()).isEqualTo(ProductEvent.class.getName());
        assertThat(outboxEvent.getEventKey()).isEqualTo(String.valueOf(savedProduct.getId()));
        assertThat(outboxEvent.getStatus()).isEqualTo(ProductOutboxStatus.PENDING);

        assertThat(payload.productId()).isEqualTo(savedProduct.getId());
        assertThat(payload.sellerId()).isEqualTo(201L);
        assertThat(payload.name()).isEqualTo("Keychron V1 Max");
        assertThat(payload.price()).isEqualTo(190_000L);
        assertThat(payload.salePrice()).isEqualTo(175_000L);
        assertThat(payload.stock()).isEqualTo(18);
        assertThat(payload.imageUrl()).isEqualTo("https://image.example/v1-after.png");
        assertThat(payload.productState()).isEqualTo("ON_SALE");
        assertThat(payload.eventType()).isEqualTo(ProductEventType.UPDATE);
    }

    @Test
    @DisplayName("deleteProduct stores a pending product.changed delete event in the outbox")
    void deleteProduct_whenSuccessful_savesPendingOutboxEvent() throws Exception {
        Product savedProduct = productRepository.save(Product.builder()
                .sellerId(301L)
                .name("Rainy75")
                .price(210_000L)
                .salePrice(199_000L)
                .stock(8)
                .category(Category.KEYBOARD)
                .description("delete target")
                .imageUrl("https://image.example/rainy75.png")
                .detail(Map.of("layout", "75%"))
                .build());

        productManageService.deleteProduct(savedProduct.getId(), 301L, MemberRole.SELLER);

        List<ProductOutboxEvent> outboxEvents = productOutboxEventRepository.findAll();

        assertThat(productRepository.existsById(savedProduct.getId())).isFalse();
        assertThat(outboxEvents).hasSize(1);

        ProductOutboxEvent outboxEvent = outboxEvents.get(0);
        ProductEvent payload = objectMapper.readValue(outboxEvent.getPayload(), ProductEvent.class);

        assertThat(outboxEvent.getTopic()).isEqualTo(KafkaTopics.PRODUCT_CHANGED);
        assertThat(outboxEvent.getEventType()).isEqualTo(ProductEvent.class.getName());
        assertThat(outboxEvent.getEventKey()).isEqualTo(String.valueOf(savedProduct.getId()));
        assertThat(outboxEvent.getStatus()).isEqualTo(ProductOutboxStatus.PENDING);

        assertThat(payload.productId()).isEqualTo(savedProduct.getId());
        assertThat(payload.sellerId()).isEqualTo(301L);
        assertThat(payload.eventType()).isEqualTo(ProductEventType.DELETE);
    }
}
