package com.thock.back.api.boundedContext.market.domain;

import com.thock.back.api.global.jpa.entity.BaseManualIdAndTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "market_cart_items")
@NoArgsConstructor
@Getter
public class CartItem extends BaseManualIdAndTime {
}
