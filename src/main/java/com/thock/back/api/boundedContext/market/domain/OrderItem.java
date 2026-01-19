package com.thock.back.api.boundedContext.market.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "market_order_items")
@Getter
@NoArgsConstructor
public class OrderItem extends BaseIdAndTime {

}
