package com.thock.back.api.boundedContext.market.out.client;

import com.thock.back.api.boundedContext.market.out.api.dto.ProductInfo;

// Outbound Port (인터페이스)
public interface ProductClient {
    ProductInfo getProduct(Long productId);
}
