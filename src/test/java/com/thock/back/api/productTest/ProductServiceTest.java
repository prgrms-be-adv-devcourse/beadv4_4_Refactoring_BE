package com.thock.back.api.productTest;

import com.thock.back.api.boundedContext.product.app.ProductService;

import com.thock.back.api.boundedContext.product.domain.Category;
import com.thock.back.api.boundedContext.product.domain.Product;
import com.thock.back.api.boundedContext.product.in.dto.ProductCreateRequest;
import com.thock.back.api.boundedContext.product.out.ProductRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;

import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.dto.MemberDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
public class ProductServiceTest {

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private EventPublisher eventPublisher;

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("상품 등록 성공 테스트 - 이름이 정상적으로 매핑되어야 함")
    void productCreate_Success() {
        // given
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("기계식 키보드");
        request.setPrice(150000L);
        request.setSalePrice(130000L);
        request.setStock(3);
        request.setCategory(Category.KEYBOARD);
        request.setDescription("게임할 때 쓰기 좋은 기계식 키보드");
        request.setImageUrl("http://image.com/1.jpg");

        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .role(MemberRole.SELLER)
                // name is purposely null or different to prove we use request.getName()
                .name("판매자 이름")
                .build();

        // when
        // Mock save to return a product with ID (stubbing)
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            // We can't easily set ID on entity if it doesn't have setter,
            // but we can just return the same object or a mocked one.
            // Let's assume we just want to verify what was passed to save.
            return p;
        });

        productService.productCreate(request, memberDto);

        // then
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();

        // This is the critical check: Name should come from request, NOT memberDto
        assertThat(capturedProduct.getName()).isEqualTo("기계식 키보드");
        assertThat(capturedProduct.getSellerId()).isEqualTo(1L);
        assertThat(capturedProduct.getPrice()).isEqualTo(150000L);
    }
}
