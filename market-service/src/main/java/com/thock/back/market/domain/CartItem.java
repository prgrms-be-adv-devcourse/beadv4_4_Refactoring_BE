package com.thock.back.market.domain;

import com.thock.back.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "market_cart_items")
@NoArgsConstructor
@Getter
public class CartItem extends BaseIdAndTime {

    @ManyToOne(fetch = LAZY)
    private Cart cart;

    // ID만 저장 (외래키 없이 그냥 Long 타입)
    private Long productId;

    private Integer quantity; // 장바구니에 담은 상품 수량

    public CartItem(Cart cart, Long productId, Integer quantity){
        this.cart = cart;
        this.productId = productId;
        this.quantity = quantity;
    }

    // 수량 변경 메서드
    public void updateQuantity(Integer quantity){
        this.quantity = quantity;
    }
}
