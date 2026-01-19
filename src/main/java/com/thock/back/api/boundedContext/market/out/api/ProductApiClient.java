package com.thock.back.api.boundedContext.market.out.api;

import com.thock.back.api.boundedContext.market.out.api.dto.ProductInfo;
import com.thock.back.api.boundedContext.market.out.client.ProductClient;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Outbound Adapter (구현체)
@Component
public class ProductApiClient implements ProductClient {
    private final RestClient restClient; // global/config/RestConfig 에서 주입됨

    public ProductApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/products")
                .build();
    }
    @Override
    public ProductInfo getProduct(Long productId) {
        return restClient.get()
                .uri("/{id}", productId)
                .retrieve()
                .body(ProductInfo.class);
    }
}
