package com.thock.back.market.out.repository;

import com.thock.back.market.domain.Order;
import com.thock.back.market.domain.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderName);
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    // 타임아웃 스케줄러용: 결제 요청 후 N분 경과한 미결제 주문 조회
    List<Order> findByStateAndRequestPaymentDateBefore(OrderState state, LocalDateTime before);

    // 미결제 주문 존재 여부 확인 (중복 주문 방지용)
    boolean existsByBuyerIdAndState(Long buyerId, OrderState state);

    // 취소 상태의 주문 며칠 지났는지 확인용 / 30일 지났을 시 배치 처리
    List<Order> findByStateAndCancelDateBefore(OrderState state, LocalDateTime before);

    long countByState(OrderState state);
}
