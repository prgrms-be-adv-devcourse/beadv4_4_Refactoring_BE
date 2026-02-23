package com.thock.back.settlement.settlement.app.useCase;

import com.thock.back.settlement.reconciliation.app.port.GetSettlementCandidatesUseCase;
import com.thock.back.settlement.reconciliation.app.port.MarkSalesLogsSettledPort;
import com.thock.back.settlement.reconciliation.app.port.SettlementCandidate;
import com.thock.back.settlement.settlement.domain.SettlementFeePolicy;
import com.thock.back.settlement.settlement.domain.DailySettlement;
import com.thock.back.settlement.settlement.out.DailySettlementRepository;
import com.thock.back.settlement.shared.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // ★ 핵심: 스프링 없이 Mockito만 씀 (0.1초 컷)
class RunDailySettlementUseCaseTest {

    @InjectMocks
    private RunDailySettlementUseCase useCase; // 테스트 대상

    @Mock
    private GetSettlementCandidatesUseCase getSettlementCandidatesUseCase; // 가짜 Port

    @Mock
    private MarkSalesLogsSettledPort markSalesLogsSettledPort; // 가짜 Port

    @Mock
    private DailySettlementRepository dailySettlementRepository; // 가짜 Repo

    @Mock
    private SettlementFeePolicy settlementFeePolicy;

    @Test
    @DisplayName("정상 흐름: 2명의 판매자 데이터를 받아 정산서 2개가 생성되고 저장되어야 한다.")
    void execute_success() {
        // given (상황 연출)
        LocalDate today = LocalDate.now();

        // 1. 테스트 데이터 준비 (A판매자: 2건, B판매자: 1건)
        // A판매자는 같은 상품을 2번 팔아서 '합계(Aggregation)'가 잘 되는지도 확인
        SettlementCandidate itemA_1 = createCandidate(1L, 100L, 101L, "키보드", 1, 10000L);
        SettlementCandidate itemA_2 = createCandidate(2L, 100L, 101L, "키보드", 1, 10000L);
        SettlementCandidate itemB_1 = createCandidate(3L, 200L, 201L, "마우스", 1, 15000L);

        List<SettlementCandidate> candidates = List.of(itemA_1, itemA_2, itemB_1);

        // 2. Mocking (가짜 행동 정의)
        // "후보군 달라하면 이거 줘라"
        given(getSettlementCandidatesUseCase.getCandidates()).willReturn(candidates);

        // "저장하라고 하면, 들어온 거 그대로 리턴해라" (save 호출 시)
        given(dailySettlementRepository.save(any(DailySettlement.class)))
                .willAnswer(invocation -> invocation.getArgument(0)); // 들어온 객체 그대로 반환

        given(settlementFeePolicy.calculateFee(any(Money.class)))
                .willAnswer(invocation -> {
                    Money payment = invocation.getArgument(0);
                    return payment.multiply(java.math.BigDecimal.valueOf(0.2));
                });

        // when (실행!)
        useCase.execute(today);

        // then (검증)

        // 1. 저장이 총 2번(판매자 A, B) 호출되었는지 확인
        // ArgumentCaptor: 메소드에 들어간 파라미터를 낚아채서 검사하는 도구
        ArgumentCaptor<DailySettlement> captor = ArgumentCaptor.forClass(DailySettlement.class);
        verify(dailySettlementRepository, times(2)).save(captor.capture());

        List<DailySettlement> savedSettlements = captor.getAllValues();

        // 2. 판매자 A (100번) 검증
        DailySettlement settlementA = savedSettlements.stream()
                .filter(s -> s.getSellerId().equals(100L))
                .findFirst().orElseThrow();

        // 총액: 10000 + 10000 = 20000
        assertThat(settlementA.getPaymentAmount().amount()).isEqualTo(20000L);
        // 수수료(20%): 4000
        assertThat(settlementA.getFeeAmount().amount()).isEqualTo(4000L);
        // 지급액: 16000
        assertThat(settlementA.getSettlementAmount().amount()).isEqualTo(16000L);
        // 아이템 개수: 2건을 합쳐서 키보드 1개가 되었는지?
        assertThat(settlementA.getItems()).hasSize(1);
        assertThat(settlementA.getItems().get(0).getFinalQuantity()).isEqualTo(2); // 수량 합산 확인

        // 3. 판매자 B (200번) 검증
        DailySettlement settlementB = savedSettlements.stream()
                .filter(s -> s.getSellerId().equals(200L))
                .findFirst().orElseThrow();

        assertThat(settlementB.getPaymentAmount().amount()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("후보군이 없으면 아무 일도 일어나지 않아야 한다.")
    void execute_empty() {
        // given
        given(getSettlementCandidatesUseCase.getCandidates()).willReturn(List.of());

        // when
        useCase.execute(LocalDate.now());

        // then
        // save 메소드가 한 번도 호출되지 않았어야 함
        verify(dailySettlementRepository, never()).save(any());
    }

    // 테스트 데이터 생성용 헬퍼 메소드
    private SettlementCandidate createCandidate(Long logId, Long sellerId, Long productId, String name, int qty, long amount) {
        return new SettlementCandidate(
                logId, sellerId, productId, name, qty, amount, "ORD-999"
        );
    }
}
