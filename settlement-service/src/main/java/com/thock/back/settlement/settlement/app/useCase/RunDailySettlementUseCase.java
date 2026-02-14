package com.thock.back.settlement.settlement.app.useCase;

import com.thock.back.settlement.reconciliation.app.port.GetSettlementCandidatesUseCase;
import com.thock.back.settlement.reconciliation.app.port.SettlementCandidate;
import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import com.thock.back.settlement.settlement.domain.DailySettlement;
import com.thock.back.settlement.settlement.domain.DailySettlementItem;
import com.thock.back.settlement.settlement.domain.SettlementFeePolicy;
import com.thock.back.settlement.settlement.out.DailySettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/* 흐름
1. settlement 모듈로부터 정산 후보군을 받아온다.
2. 해당 정산 후보군들은 이벤트 소싱이 적용돼있기 때문에 판매자별로 그루핑을 시켜줘야한다.
3. 판매자 별로 정산서(일별정산), 상세내역(일별 판매내역)을 생성해 줘야한다.
3-1. 부모 정산서를 만든다.

 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RunDailySettlementUseCase {

    private final GetSettlementCandidatesUseCase getSettlementCandidatesUseCase;
    private final SalesLogRepository salesLogRepository;
    private final DailySettlementRepository dailySettlementRepository;
    private final SettlementFeePolicy settlementFeePolicy;

    @Transactional
    public void execute(LocalDate targetDate) {
        log.info("=========[일별 정산 시작] 기준일: {} =========", targetDate);

        // 1. 정산 대상 조회 (Port 이용 호출)
        List<SettlementCandidate> candidates = getSettlementCandidatesUseCase.getCandidates();
        if (candidates.isEmpty()) {
            log.info("정산할 대상이 없습니다.");
            return;
        }

        // 2. 판매자(SellerId) 별로 그룹핑
        Map<Long, List<SettlementCandidate>> groupsBySeller = candidates.stream()
                .collect(Collectors.groupingBy(SettlementCandidate::sellerId));

        // 3. 각 판매자별로 정산서와 상세내역 생성 및 저장
        for (Long sellerId : groupsBySeller.keySet()) {

            // 판매자별로 그루핑된 맵의 value값
            List<SettlementCandidate> sellerItems = groupsBySeller.get(sellerId);

            // 3-1. 부모 정산서 생성
            DailySettlement settlement = DailySettlement.create(sellerId, targetDate);

            // 3-2. 자식 아이템 생성
            processSettlementItems(settlement, sellerItems);

            // 3-3. 금액 계산
            settlement.calculateTotalAmount(settlementFeePolicy);

            // 4. DB 저장
            DailySettlement savedSettlement = dailySettlementRepository.save(settlement);

            //5. Write-back: JPA Dirty Checking (N+1 문제 재현)

            // DTO에는 엔티티의 생명주기가 없으므로, ID를 추출해 엔티티를 다시 조회해야 함.

            // 5-1. ID 추출
            List<Long> logIds = sellerItems.stream()
                    .map(SettlementCandidate::salesLogId)
                    .toList();

            // 5-2. 엔티티 조회 (SELECT 발생) - 여기서 영속성 컨텍스트에 올라감
            List<SalesLog> targetLogs = salesLogRepository.findAllById(logIds);

            // 5-3. 상태 변경 (트랜잭션 커밋 시점에 UPDATE 쿼리 발생)
            targetLogs.forEach(log -> log.markAsSettled(savedSettlement.getId()));
        }

        log.info("=========[일별 정산 종료] 처리된 판매자 수: {} =========", groupsBySeller.size());
    }


    // 1.정산 후보군 -> 2.정산 후 결과 -> 3.정산 상세내역서
    // 아래의 메소드는 2->3 으로의 변환을 도와줌

    private void processSettlementItems(DailySettlement settlement, List<SettlementCandidate> candidates) {

        // 특정 판매자에게 들어온 주문서들을 상품별로 그루핑
        Map<Long, List<SettlementCandidate>> itemsByProduct = candidates.stream()
                .collect(Collectors.groupingBy(SettlementCandidate::productId));

        // 상품별로 그루핑된 데이터들을 저장하여 세부내역서 생성. 이 때, 결제/환불로 0원처리 된 것들은 제외.
        for (List<SettlementCandidate> productLogs : itemsByProduct.values()) {
            DailySettlementItem item = DailySettlementItem.from(productLogs);
            if (item.getFinalQuantity() == 0 && item.getFinalAmount().isZero()) {
                continue;
            }
            settlement.addItem(item);
        }
    }
}
