package com.thock.back.api.boundedContext.product.app;

import com.thock.back.api.boundedContext.product.domain.Category;
import com.thock.back.api.boundedContext.product.domain.Product;
import com.thock.back.api.boundedContext.product.in.dto.ProductCreateRequest;
import com.thock.back.api.boundedContext.product.in.dto.ProductDetailResponse;
import com.thock.back.api.boundedContext.product.in.dto.ProductListResponse;
import com.thock.back.api.boundedContext.product.in.dto.ProductUpdateRequest;
import com.thock.back.api.boundedContext.product.out.ProductRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.dto.MemberDto;
import com.thock.back.api.shared.product.event.ProductEvent;
import com.thock.back.api.shared.product.event.ProductEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;

    // 상품 등록(C). 이후 등록한 사람의 ID를 반환
    @Transactional
    public Long productCreate(ProductCreateRequest request, MemberDto memberDto) {
        if (memberDto.getRole() != MemberRole.SELLER) {
            throw new CustomException(ErrorCode.USER_FORBIDDEN);
        }
        Product product = Product.builder()
                .sellerId(memberDto.getId())
                .name(request.getName())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .detail(request.getDetail())
                .build();
        Product savedProduct = productRepository.save(product);

        eventPublisher.publish(ProductEvent.builder()
                .productId(savedProduct.getId())
                .sellerId(savedProduct.getSellerId())
                .eventType(ProductEventType.CREATE) // 중요: 타입 명시
                .build());
        return savedProduct.getId();
    }

    // 카테고리를 통해 상품 조회(R)
    @Transactional(readOnly = true)
    public List<ProductListResponse> searchByCategory(Category category) {
        List<Product> products = productRepository.findByCategory(category);

        return products.stream()
                .map(ProductListResponse::new)
                .toList();
    }

    // 특정 상품의 id를 통해 해당 상품의 상세정보 조회(R)
    @Transactional(readOnly = true)
    public ProductDetailResponse productDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품을 찾을 수 없습니다. id=" + id));
        return new ProductDetailResponse(product);
    }

    // 상품 정보 업데이트(U)
    @Transactional
    public Long productUpdate(Long productId, ProductUpdateRequest request, MemberDto memberDto) {
        // 상품 존재 하는지 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        // 본인의 상품이 맞는지 확인
        if (!product.getSellerId().equals(memberDto.getId())) {
            throw new CustomException(ErrorCode.SELLER_FORBIDDEN);
        }

        // 수정
        product.modify(
                request.getName(),
                request.getPrice(),
                request.getSalePrice(),
                request.getStock(),
                request.getCategory(),
                request.getDescription(),
                request.getImageUrl(),
                request.getDetail());

        // 이벤트 발행. 마켓모듈에 알림
        eventPublisher.publish(ProductEvent.builder()
                .productId(product.getId())
                .sellerId(product.getSellerId())
                .name(product.getName())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .productState(product.getState().name())
                .eventType(ProductEventType.UPDATE)
                .build());

        return product.getId();
    }

    // 상품 삭제(D)
    public void productDelete(Long productId, MemberDto memberDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.getSellerId().equals(memberDto.getId()) || memberDto.getRole() == MemberRole.USER) {
            throw new CustomException(ErrorCode.SELLER_FORBIDDEN);
        }

        Long deletedId = product.getId();
        Long sellerId = product.getSellerId();
        productRepository.delete(product);

        eventPublisher.publish(ProductEvent.builder()
                .productId(deletedId)
                .sellerId(sellerId)
                .eventType(ProductEventType.DELETE)
                .build());
    }

    // 키워드 검색
    public List<ProductListResponse> searchByKeyword(String keyword){
        if(keyword == null || keyword.isBlank()){
            return List.of();
        }
        return productRepository.findByNameContaining(keyword).stream()
                .map(ProductListResponse::new)
                .toList();
    }
}