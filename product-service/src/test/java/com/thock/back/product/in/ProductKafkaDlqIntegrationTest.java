package com.thock.back.product.in;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.kafka.KafkaTopics;
import com.thock.back.product.ProductServiceApplication;
import com.thock.back.product.app.ProductStockService;
import com.thock.back.shared.market.domain.StockEventType;
import com.thock.back.shared.market.dto.StockOrderItemDto;
import com.thock.back.shared.market.event.MarketOrderStockChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = {
                ProductServiceApplication.class,
                ProductKafkaDlqIntegrationTest.DlqTestConfig.class
        },
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.listener.auto-startup=true",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "inbox.enabled=false"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = {
                KafkaTopics.MARKET_ORDER_STOCK_CHANGED,
                KafkaTopics.MARKET_ORDER_STOCK_CHANGED_DLQ
        }
)
@ActiveProfiles("test")
@DirtiesContext
class ProductKafkaDlqIntegrationTest {

    @MockitoBean
    private ProductStockService productStockService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private DlqProbe dlqProbe;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @BeforeEach
    void setUp() {
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }

    @AfterEach
    void tearDown() {
        dlqProbe.clear();
        reset(productStockService);
    }

    // 정상 처리 시 DLQ로 가지 않고 정상적으로 처리
    @Test
    @DisplayName("successful processing does not publish to the DLQ")
    void handle_whenProcessingSucceeds_doesNotSendToDlq() throws Exception {
        MarketOrderStockChangedEvent event = stockChangedEvent("ORDER-SUCCESS");
        doNothing().when(productStockService).handle(any(MarketOrderStockChangedEvent.class));

        kafkaTemplate.send(KafkaTopics.MARKET_ORDER_STOCK_CHANGED, event).get(5, TimeUnit.SECONDS);

        verify(productStockService, timeout(5000).times(1)).handle(any(MarketOrderStockChangedEvent.class));
        assertThat(dlqProbe.poll(1500, TimeUnit.MILLISECONDS)).isNull();
    }

    // CustomException 발생 시 재시도 없이 즉시 DLQ 전송
    @Test
    @DisplayName("CustomException is sent to the DLQ without retries")
    void handle_whenCustomExceptionOccurs_sendsToDlqImmediately() throws Exception {
        MarketOrderStockChangedEvent event = stockChangedEvent("ORDER-CUSTOM-ERROR");
        doThrow(new CustomException(ErrorCode.INVALID_REQUEST))
                .when(productStockService)
                .handle(any(MarketOrderStockChangedEvent.class));

        kafkaTemplate.send(KafkaTopics.MARKET_ORDER_STOCK_CHANGED, event).get(5, TimeUnit.SECONDS);

        DlqMessage dlqMessage = dlqProbe.poll(10, TimeUnit.SECONDS);

        assertThat(dlqMessage).isNotNull();
        assertThat(dlqMessage.payload().orderNumber()).isEqualTo(event.orderNumber());
        assertThat(dlqMessage.originalTopic()).isEqualTo(KafkaTopics.MARKET_ORDER_STOCK_CHANGED);
        assertThat(dlqMessage.exceptionClass()).contains("ListenerExecutionFailedException");
        assertThat(dlqMessage.causeClass()).contains(CustomException.class.getName());
        verify(productStockService, timeout(5000).times(1)).handle(any(MarketOrderStockChangedEvent.class));
    }

    // 일반 RuntimeException 발생 시 최초 1회 + 재시도 2회 후 DLQ 전송 (총 3회 시도)
    @Test
    @DisplayName("RuntimeException is retried and then sent to the DLQ")
    void handle_whenRuntimeExceptionOccurs_retriesThenSendsToDlq() throws Exception {
        MarketOrderStockChangedEvent event = stockChangedEvent("ORDER-RUNTIME-ERROR");
        doThrow(new RuntimeException("temporary failure"))
                .when(productStockService)
                .handle(any(MarketOrderStockChangedEvent.class));

        kafkaTemplate.send(KafkaTopics.MARKET_ORDER_STOCK_CHANGED, event).get(5, TimeUnit.SECONDS);

        DlqMessage dlqMessage = dlqProbe.poll(15, TimeUnit.SECONDS);

        assertThat(dlqMessage).isNotNull();
        assertThat(dlqMessage.payload().orderNumber()).isEqualTo(event.orderNumber());
        assertThat(dlqMessage.originalTopic()).isEqualTo(KafkaTopics.MARKET_ORDER_STOCK_CHANGED);
        assertThat(dlqMessage.exceptionClass()).contains("ListenerExecutionFailedException");
        assertThat(dlqMessage.causeClass()).contains(RuntimeException.class.getName());
        verify(productStockService, timeout(10000).times(3)).handle(any(MarketOrderStockChangedEvent.class));
    }

    private MarketOrderStockChangedEvent stockChangedEvent(String orderNumber) {
        return new MarketOrderStockChangedEvent(
                orderNumber,
                StockEventType.RESERVE,
                java.util.List.of(new StockOrderItemDto(1L, 2))
        );
    }

    @TestConfiguration
    static class DlqTestConfig {

        @Bean
        DlqProbe dlqProbe() {
            return new DlqProbe();
        }
    }

    static class DlqProbe {

        private final BlockingQueue<DlqMessage> messages = new LinkedBlockingQueue<>();

        @KafkaListener(
                topics = KafkaTopics.MARKET_ORDER_STOCK_CHANGED_DLQ,
                groupId = "product-service-dlq-test"
        )
        public void listen(
                MarketOrderStockChangedEvent payload,
                @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic,
                @Header(KafkaHeaders.DLT_EXCEPTION_FQCN) String exceptionClass,
                @Header(KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN) String causeClass
        ) {
            messages.offer(new DlqMessage(payload, originalTopic, exceptionClass, causeClass));
        }

        DlqMessage poll(long timeout, TimeUnit unit) throws InterruptedException {
            return messages.poll(timeout, unit);
        }

        void clear() {
            messages.clear();
        }
    }

    record DlqMessage(
            MarketOrderStockChangedEvent payload,
            String originalTopic,
            String exceptionClass,
            String causeClass
    ) {
    }
}
