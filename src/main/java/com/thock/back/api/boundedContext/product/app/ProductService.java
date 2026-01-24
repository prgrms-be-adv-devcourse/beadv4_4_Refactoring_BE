package com.thock.back.api.boundedContext.product.app;

import com.thock.back.api.boundedContext.product.domain.Category;
import com.thock.back.api.boundedContext.product.domain.Product;
import com.thock.back.api.boundedContext.product.in.dto.ProductCreateRequest;
import com.thock.back.api.boundedContext.product.in.dto.ProductDetailResponse;
import com.thock.back.api.boundedContext.product.in.dto.ProductListResponse;
import com.thock.back.api.boundedContext.product.in.dto.ProductUpdateRequest;
import com.thock.back.api.boundedContext.product.in.dto.internal.ProductInternalResponse;
import com.thock.back.api.boundedContext.product.out.ProductRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.dto.MemberDto;
import com.thock.back.api.shared.product.event.ProductEvent;
import com.thock.back.api.shared.product.event.ProductEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Long productCreate(ProductCreateRequest request, Long sellerId, MemberRole role) {

        // 1. 권한 검증 (서비스에서 비즈니스 로직으로 체크)
        if (role != MemberRole.SELLER) {
            throw new CustomException(ErrorCode.USER_FORBIDDEN);
        }

        // 2. 엔티티 생성
        Product product = Product.builder()
                .sellerId(sellerId)  // 넘겨받은 ID 사용
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

        // 3. 이벤트 발행
        eventPublisher.publish(ProductEvent.builder()
                .productId(savedProduct.getId())
                .sellerId(savedProduct.getSellerId())
                .eventType(ProductEventType.CREATE)
                .build());

        return savedProduct.getId();
    }

    // 카테고리를 통해 상품 조회(R)
    @Transactional(readOnly = true)
    public Page<ProductListResponse> searchByCategory(Category category, Pageable pageable) {
        Page<Product> productPage = productRepository.findByCategory(category, pageable);
        return productPage.map(ProductListResponse::new);
    }

    // 특정 상품의 id를 통해 해당 상품의 상세정보 조회(R)
    @Transactional(readOnly = true)
    public ProductDetailResponse productDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품을 찾을 수 없습니다. id=" + id));
        return new ProductDetailResponse(product);
    }

    // 4. 상품 정보 업데이트(U)
    @Transactional
    public Long productUpdate(Long productId, ProductUpdateRequest request, Long memberId, MemberRole role) {
        // 1. 상품 존재 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. 권한 검증 (관리자(ADMIN)가 아니고, 내 상품(sellerId)도 아니면 에러)
        // role == ADMIN 이면 통과, 아니면 ID 비교
        if (role != MemberRole.ADMIN && !product.getSellerId().equals(memberId)) {
            throw new CustomException(ErrorCode.SELLER_FORBIDDEN);
        }

        // 3. 수정 (Dirty Checking)
        product.modify(
                request.getName(),
                request.getPrice(),
                request.getSalePrice(),
                request.getStock(),
                request.getCategory(),
                request.getDescription(),
                request.getImageUrl(),
                request.getDetail()
        );

        // 4. 이벤트 발행
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

    // 5. 상품 삭제(D)
    @Transactional // 삭제도 트랜잭션 필수!
    public void productDelete(Long productId, Long memberId, MemberRole role) {
        // 1. 상품 존재 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. 권한 검증 (관리자가 아니고, 내 상품도 아니면 에러)
        if (role != MemberRole.ADMIN && !product.getSellerId().equals(memberId)) {
            throw new CustomException(ErrorCode.SELLER_FORBIDDEN);
        }

        // 3. 데이터 보존을 위해 변수에 담아둠 (삭제 후엔 접근 못하니까)
        Long deletedId = product.getId();
        Long sellerId = product.getSellerId();

        // 4. 삭제
        productRepository.delete(product);

        // 5. 이벤트 발행
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

    // 장바구니 상세정보 조회 기능

    @Transactional(readOnly = true)
    public List<ProductInternalResponse> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        // WHERE ID IN 과 같은 메소드
        List<Product> products = productRepository.findAllByIdIn(productIds);

        // 가져온 product를 productInternalResponse에 맞게 알아서 변환해줌
        return products.stream()
                .map(ProductInternalResponse::new) // 생성자로 변환
                .toList();
    }
}