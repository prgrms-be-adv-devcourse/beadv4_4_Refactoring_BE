package com.thock.back.product.config.kafka;

import com.thock.back.global.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class ProductKafkaTopicConfig {

    @Bean
    public NewTopic productChangedTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCT_CHANGED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    // DLQ 전용 토픽 생성
    @Bean
    public NewTopic marketOrderStockChangedDlqTopic() {
        return TopicBuilder.name(KafkaTopics.MARKET_ORDER_STOCK_CHANGED_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
