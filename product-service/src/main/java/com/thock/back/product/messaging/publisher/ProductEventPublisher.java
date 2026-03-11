package com.thock.back.product.messaging.publisher;

import com.thock.back.shared.product.event.ProductEvent;

public interface ProductEventPublisher {
    void publish(ProductEvent event);
}
