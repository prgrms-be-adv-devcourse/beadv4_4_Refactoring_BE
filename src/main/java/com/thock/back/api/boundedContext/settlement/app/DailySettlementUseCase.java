package com.thock.back.api.boundedContext.settlement.app;

import com.thock.back.api.boundedContext.settlement.domain.*;
import com.thock.back.api.boundedContext.settlement.out.SettlementMemberRepository;
import com.thock.back.api.boundedContext.settlement.out.SettlementRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher; // 👈 사용자님이 만든 커스텀 퍼블리셔
import com.thock.back.api.shared.settlement.dto.SettlementOrderDto;
import com.thock.back.api.shared.settlement.event.SettlementCompletedEvent; // 👈 Shared 이벤트
import com.thock.back.api.shared.settlement.port.MarketDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service; // UseCase는 Service 역할
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailySettlementUseCase {

    private final SettlementRepository settlementRepository;
    private final SettlementMemberRepository settlementMemberRepository;
    private final MarketDataPort marketDataPort;
    private final EventPublisher eventPublisher; // 👈 사용자님의 그 클래스!

    @Transactional
    public void executeProcess(LocalDate date) {
        // 1. 판매자 조회
        List<SettlementMember> sellers = settlementMemberRepository.findAll();

        for (SettlementMember seller : sellers) {
            try {
                processOneSeller(seller, date);
            } catch (Exception e) {
                log.error("정산 실패 - sellerId: {}", seller.getId(), e);
            }
        }
    }

    private void processOneSeller(SettlementMember seller, LocalDate date) {
        // 중복 체크 등 기존 로직 ...
        if (settlementRepository.findByMemberIdAndSalesDate(seller.getId(), date).isPresent()) return;

        // 1. 마켓 데이터 가져오기
        List<SettlementOrderDto> orders = marketDataPort.getSettlementTargetOrders(seller.getId(), date);
        if (orders.isEmpty()) return;

        // 2. 금액 계산
        long totalAmount = orders.stream().mapToLong(SettlementOrderDto::getTotalSalePrice).sum();
        long payoutAmount = orders.stream().mapToLong(SettlementOrderDto::getPayoutAmount).sum();
        long feeAmount = orders.stream().mapToLong(SettlementOrderDto::getFeeAmount).sum();

        // 3. 엔티티 생성 및 저장
        Settlement settlement = Settlement.builder()
                .member(seller)
                .salesDate(date)
                .totalAmount(totalAmount)
                .payoutAmount(payoutAmount)
                .feeAmount(feeAmount)
                .appliedFeeRate(5.0)
                .build();

        // 4. [상세 내역 추가]
        for (SettlementOrderDto orderDto : orders) {
            SettlementDetail detail = SettlementDetail.builder()
                    .orderId(orderDto.getOrderId())         // 주문 번호
                    .orderItemId(orderDto.getOrderItemId()) // 주문 상품 번호
                    .productId(orderDto.getProductId())     // 상품 ID
                    .productName(orderDto.getProductName()) // 상품명 (스냅샷)
                    .quantity(orderDto.getQuantity())       // 수량
                    .paymentAmount(orderDto.getTotalSalePrice()) // 결제 금액
                    .payoutAmount(orderDto.getPayoutAmount())    // 정산 금액
                    .fee(orderDto.getFeeAmount())                // 수수료
                    .build();

            // Settlement(부모)에 Detail(자식)을 연결
            settlement.addDetail(detail);
        }
        // ==========================================

        // 5. 저장 (이때 마스터와 디테일이 같이 저장됨 - Cascade 설정 덕분)
        settlementRepository.save(settlement);

        // 4. [핵심] 이벤트 발행! 📢
        // "정산 끝났으니 돈 주세요!"
        eventPublisher.publish(new SettlementCompletedEvent(seller.getId(), payoutAmount));

        // 5. 정산 완료 처리
        settlement.complete(LocalDateTime.now());

        log.info("정산 완료 이벤트 발행됨: sellerId={}, amount={}", seller.getId(), payoutAmount);
    }
}