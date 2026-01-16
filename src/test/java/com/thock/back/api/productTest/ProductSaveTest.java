package com.thock.back.api.productTest;


import com.thock.back.api.boundedContext.product.domain.Category;
import com.thock.back.api.boundedContext.product.domain.Product;
import com.thock.back.api.boundedContext.product.out.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class ProductSaveTest {
    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품 등록 테스트")
    void productSaveTest() {
        Product product = Product.builder()
                .sellerId(1L)
                .name("테스트 키보드")
                .price(50000L)
                .description("기계식 키보드입니다.")
                .category(Category.KEYBOARD)
                .build();

        Product savedProduct = productRepository.save(product);

        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("테스트 키보드");
        assertThat(savedProduct.getPrice()).isEqualTo(50000L);
    }
}
