package com.thock.back.market.domain;

import com.thock.back.global.jpa.entity.BaseIdAndTime;
import com.thock.back.shared.market.domain.CancelReasonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "market_order_cancel_history",
        indexes = {
                @Index(name = "idx_cancel_history_order", columnList = "order_id"),
                @Index(name = "idx_cancel_history_order_item", columnList = "order_item_id")
        }
)
@Getter
@NoArgsConstructor
public class OrderCancelHistory extends BaseIdAndTime {

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Enumerated(STRING)
    @Column(nullable = false)
    private CancelReasonType cancelReasonType;

    private String cancelReasonDetail;

    @Enumerated(STRING)
    @Column(nullable = false)
    private CancelledBy cancelledBy;

    // 나중에 CS에서 사용자 취소율, 시스템 취소율 판단 가능 : 운영 관점
    public enum CancelledBy {
        USER,   // 사용자가 취소
        SYSTEM  // 시스템이 취소 (타임아웃 등)
    }

    private OrderCancelHistory(Order order, OrderItem orderItem,
                               CancelReasonType cancelReasonType,
                               String cancelReasonDetail, CancelledBy cancelledBy) {
        this.order = order;
        this.orderItem = orderItem;
        this.cancelReasonType = cancelReasonType;
        this.cancelReasonDetail = cancelReasonDetail;
        this.cancelledBy = cancelledBy;
    }

    // 팩토리 메서드: 사용자 취소
    public static OrderCancelHistory ofUserCancel(Order order, OrderItem orderItem,
                                                  CancelReasonType cancelReasonType,
                                                  String cancelReasonDetail) {
        return new OrderCancelHistory(order, orderItem, cancelReasonType, cancelReasonDetail, CancelledBy.USER);
    }

    // 팩토리 메서드: 시스템 취소
    public static OrderCancelHistory ofSystemCancel(Order order, OrderItem orderItem,
                                                    CancelReasonType cancelReasonType) {
        return new OrderCancelHistory(order, orderItem, cancelReasonType, "시스템에서 취소(타임 아웃 등)", CancelledBy.SYSTEM);
    }
}
