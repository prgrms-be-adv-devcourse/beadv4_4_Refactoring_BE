package com.thock.back.api.boundedContext.product.app;

import com.thock.back.api.boundedContext.product.domain.Category;
import com.thock.back.api.boundedContext.product.domain.Product;
import com.thock.back.api.boundedContext.product.in.dto.ProductDetailResponse;
import com.thock.back.api.boundedContext.product.in.dto.ProductListResponse;
import com.thock.back.api.boundedContext.product.out.ProductRepository;
import com.thock.back.api.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;


    // 카테고리를 통해 상품 조회
    @Transactional(readOnly = true)
    public List<ProductListResponse> searchByCategory(Category category){
        List<Product> products = productRepository.findByCategory(category);

        return products.stream()
                .map(ProductListResponse::new)
                .toList();
    }

    // 특정 상품의 id를 통해 해당 상품의 상세정보 조회
    @Transactional(readOnly = true)
    public ProductDetailResponse productDetail(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품을 찾을 수 없습니다. id=" + id));
        return new ProductDetailResponse(product);
        }
    }


//    @Transactional
//    public
