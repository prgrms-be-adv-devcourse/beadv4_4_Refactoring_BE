package com.thock.back.api.boundedContext.market.out.client;

import com.thock.back.api.boundedContext.market.out.api.dto.ProductInfo;

import java.util.List;

// Outbound Port (인터페이스)
public interface ProductClient {
    List<ProductInfo> getProducts(List<Long> productIds);
}
