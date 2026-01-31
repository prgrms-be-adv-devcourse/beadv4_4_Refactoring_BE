package com.thock.back.product.domain;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.jpa.entity.BaseIdAndTime;
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
    // @Column(columnDefinition = "TEXT")
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
        this.imageUrl = imageUrl;
        this.detail = detail;

        // 시스템 설정 기본값
        this.state = ProductState.ON_SALE;
        this.saleStartedAt = LocalDateTime.now();
        this.viewCount = 0L;
    }

    public void modify(String name, Long price, Long salePrice, Integer stock,
                       Category category, String description, String imageUrl,
                       Map<String, Object> detail){
        this.name = name;
        this.price = price;
        this.salePrice = salePrice;
        this.stock = stock;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.detail = detail;
    }
}