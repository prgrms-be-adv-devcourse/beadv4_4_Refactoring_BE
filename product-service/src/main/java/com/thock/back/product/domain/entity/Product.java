package com.thock.back.product.domain.entity;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.jpa.entity.BaseIdAndTime;
import com.thock.back.product.domain.Category;
import com.thock.back.product.domain.ProductState;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product extends BaseIdAndTime {
    //상품 관련 필드
    @Column(nullable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Lob
    private String description;

    private Long price;
    private Long salePrice;
    private Integer stock;
    @Column(nullable = false)
    private Integer reservedStock;
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> detail;

    @Enumerated(EnumType.STRING)
    private ProductState state;
    private LocalDateTime saleStartedAt;
    private LocalDateTime saleEndedAt;
    private Long viewCount;



    @Builder
    public Product(Long sellerId, Category category, String name, String description,
                   Long price, Long salePrice, Integer stock, String imageUrl, Map<String, Object> detail) {

        if (sellerId == null) {
            throw new CustomException(ErrorCode.SELLER_REQUIRED);
        }
        if (category == null){
            throw new CustomException(ErrorCode.PRODUCT_CATEGORY_REQUIRED);
        }
        if (name == null) {
            throw new CustomException(ErrorCode.PRODUCT_NAME_REQUIRED);
        }
        if (price == null) {
            throw new CustomException(ErrorCode.PRODUCT_PRICE_INVALID);
        }

        this.sellerId = sellerId;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.salePrice = (salePrice != null) ? salePrice : price;
        this.stock = (stock != null) ? stock : 0;
        this.reservedStock = 0;
        this.imageUrl = imageUrl;
        this.detail = detail;

        // 시스템 설정 기본값
        this.state = ProductState.ON_SALE;
        this.saleStartedAt = LocalDateTime.now();
        this.viewCount = 0L;
    }

    public void modify(String name,
                       Long price,
                       Long salePrice,
                       Integer stock,
                       Category category,
                       String description,
                       String imageUrl,
                       Map<String, Object> detail){

        if (name == null || name.isBlank()) {
            throw new CustomException(ErrorCode.PRODUCT_NAME_REQUIRED);
        }

        if (price == null || price <= 0) {
            throw new CustomException(ErrorCode.PRODUCT_PRICE_INVALID);
        }

        if (category == null) {
            throw new CustomException(ErrorCode.PRODUCT_CATEGORY_REQUIRED);
        }

        this.name = name;
        this.price = price;
        this.salePrice = (salePrice != null) ? salePrice : price;
        this.stock = (stock != null) ? stock : 0;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.detail = detail;
    }

    // 재고 관리 메서드
    public void reserve(Integer quantity) {

        // 수량 유효성 검사
        validateQuantity(quantity);

        // 사용 가능한 재고 계산 (총 재고 - 이미 예약된 재고)
        int available = this.stock - this.reservedStock;

        // 사용 가능한 재고가 요청된 수량보다 적으면 예외 발생
        if (available < quantity) {
            throw new CustomException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 예약된 재고 증가
        this.reservedStock += quantity;
    }

    // 예약 해제 메서드
    public void release(Integer quantity) {

        // 수량 유효성 검사
        validateQuantity(quantity);

        // 예약된 재고가 요청된 수량보다 적으면 예외 발생
        if (this.reservedStock < quantity) {
            throw new CustomException(ErrorCode.PRODUCT_RESERVED_STOCK_NOT_ENOUGH);
        }

        // 예약된 재고 감소
        this.reservedStock -= quantity;
    }

    // 구매 확정 메서드
    public void commit(Integer quantity) {

        // 수량 유효성 검사
        validateQuantity(quantity);

        // 예약된 재고가 요청된 수량보다 적으면 예외 발생
        if (this.reservedStock < quantity) {
            throw new CustomException(ErrorCode.PRODUCT_RESERVED_STOCK_NOT_ENOUGH);
        }

        // 실제 재고가 요청된 수량보다 적으면 예외 발생 (이론적으로는 발생하지 않아야 함)
        if (this.stock < quantity) {
            throw new CustomException(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH);
        }

        // 예약된 재고와 실제 재고 모두에서 요청된 수량만큼 감소
        this.reservedStock -= quantity;
        this.stock -= quantity;

        // 재고가 0이 되면 품절 상태로 변경
        if (this.stock == 0) {
            this.state = ProductState.SOLD_OUT;
        }
    }

    // 재고 수량 유효성 검사 메서드
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CustomException(ErrorCode.PRODUCT_STOCK_QUANTITY_INVALID);
        }
    }
}