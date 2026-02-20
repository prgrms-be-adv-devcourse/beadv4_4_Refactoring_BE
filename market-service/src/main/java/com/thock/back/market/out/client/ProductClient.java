package com.thock.back.market.out.client;

import com.thock.back.market.out.api.dto.ProductInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-client", url = "${custom.global.productServiceUrl}/api/v1/products/internal")
public interface ProductClient {

    @PostMapping("/list")
    List<ProductInfo> getProducts(@RequestBody List<Long> productIds);
}
