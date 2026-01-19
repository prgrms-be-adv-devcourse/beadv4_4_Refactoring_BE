package com.thock.back.api.boundedContext.market.domain;

import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.global.jpa.entity.BaseManualIdAndTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "market_carts")
@NoArgsConstructor
@Getter
public class Cart extends BaseManualIdAndTime {
    @ManyToOne(fetch = LAZY)
    private MarketMember buyer;

    @OneToMany(mappedBy = "cart", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    private int itemsCount;

    public Cart(MarketMember buyer){
        super(buyer.getId());
        this.buyer = buyer;
    }

    public boolean hasItems() {
        return itemsCount > 0;
    }

    // 장바구니에 상품 등록
    public void addItem(Long productId, int quantity) {
        CartItem cartItem = new CartItem(this, productId, quantity);
        this.items.add(cartItem);
        this.itemsCount++;
    }

    public void updateItemQuantity(Long productId, int quantity) {
        CartItem cartItem = findItemByProductId(productId);
        cartItem.updateQuantity(quantity); // CartItem 메서드 호출
    }

    // 장바구니 내의 모든 상품을 삭제
    public void clearItems(){
        this.getItems().clear(); // items만 비움
        this.itemsCount = 0;
    }

    // 장바구니에서 특정 상품을 삭제
    public void removeItem(Long productId) {
        CartItem cartItem = findItemByProductId(productId);
        this.items.remove(cartItem);  // orphanRemoval로 DB에서도 삭제
        this.itemsCount--;
    }

    private CartItem findItemByProductId(Long productId){
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));
    }
}

