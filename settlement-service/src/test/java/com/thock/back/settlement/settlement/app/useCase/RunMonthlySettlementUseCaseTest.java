package com.thock.back.settlement.settlement.app.useCase;

import com.thock.back.settlement.settlement.domain.DailySettlement;
import com.thock.back.settlement.settlement.domain.MonthlySettlement;
import com.thock.back.settlement.settlement.domain.enums.MonthlySettlementStatus;
import com.thock.back.settlement.shared.money.Money;
import com.thock.back.settlement.settlement.out.DailySettlementRepository;
import com.thock.back.settlement.settlement.out.MonthlySettlementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // 스프링 없이 Mockito만 사용 (속도 빠름)
class RunMonthlySettlementUseCaseTest {

    @InjectMocks
    private RunMonthlySettlementUseCase useCase;

    @Mock
    private DailySettlementRepository dailySettlementRepository;

    @Mock
    private MonthlySettlementRepository monthlySettlementRepository;

    @Test
    @DisplayName("정상 흐름: 2월달 일별 정산 데이터를 모아서 판매자별 월별 정산서가 생성되어야 한다.")
    void execute_success() {
        // given (상황: 2026년 2월 정산)
        YearMonth targetMonth = YearMonth.of(2026, 2);

        // 테스트 데이터 준비 (판매자 A: 2건, 판매자 B: 1건)
        // Seller A: 1일(100원), 2일(200원) -> 총 300원 예상
        DailySettlement a1 = createDailySettlement(100L, 10000L, 2000L, 8000L);
        DailySettlement a2 = createDailySettlement(100L, 20000L, 4000L, 16000L);

        // Seller B: 5일(500원) -> 총 500원 예상
        DailySettlement b1 = createDailySettlement(200L, 50000L, 10000L, 40000L);

        List<DailySettlement> mockDailyData = List.of(a1, a2, b1);

        // Mocking: "2월 1일~28일 데이터 달라고 하면 이거 줘라"
        given(dailySettlementRepository.findAllByTargetDateBetween(any(), any()))
                .willReturn(mockDailyData);

        // when (실행)
        useCase.execute(targetMonth);

        // then (검증)

        // 1. 저장이 총 2번(판매자 A, B) 일어났는지 확인
        ArgumentCaptor<MonthlySettlement> captor = ArgumentCaptor.forClass(MonthlySettlement.class);
        verify(monthlySettlementRepository, times(2)).save(captor.capture());

        List<MonthlySettlement> savedResults = captor.getAllValues();

        // 2. 판매자 A (ID: 100) 검증 - 합계가 맞는지?
        MonthlySettlement settlementA = savedResults.stream()
                .filter(s -> s.getSellerId().equals(100L))
                .findFirst().orElseThrow();

        assertThat(settlementA.getTargetYearMonth()).isEqualTo("202602");
        assertThat(settlementA.getTotalCount()).isEqualTo(2L); // 2건
        assertThat(settlementA.getTotalPaymentAmount().amount()).isEqualTo(30000L); // 10000 + 20000
        assertThat(settlementA.getTotalFeeAmount().amount()).isEqualTo(6000L);      // 2000 + 4000
        assertThat(settlementA.getTotalPayoutAmount().amount()).isEqualTo(24000L);  // 8000 + 16000
        assertThat(settlementA.getStatus()).isEqualTo(MonthlySettlementStatus.PENDING);

        // 3. 판매자 B (ID: 200) 검증
        MonthlySettlement settlementB = savedResults.stream()
                .filter(s -> s.getSellerId().equals(200L))
                .findFirst().orElseThrow();

        assertThat(settlementB.getTotalPayoutAmount().amount()).isEqualTo(40000L);
    }

    @Test
    @DisplayName("데이터가 없으면 저장이 일어나지 않아야 한다.")
    void execute_empty() {
        // given
        given(dailySettlementRepository.findAllByTargetDateBetween(any(), any()))
                .willReturn(List.of()); // 빈 리스트 반환

        // when
        useCase.execute(YearMonth.now());

        // then
        verify(monthlySettlementRepository, times(0)).save(any());
    }

    // --- Helper Method ---
    private DailySettlement createDailySettlement(Long sellerId, Long payment, Long fee, Long settlement) {
        // DailySettlement는 @Builder가 있으므로 빌더 사용
        return DailySettlement.builder()
                .sellerId(sellerId)
                .targetDate(LocalDate.now()) // 날짜는 중요하지 않음 (기간 내 조회되었다고 가정)
                .paymentAmount(Money.of(payment))
                .feeAmount(Money.of(fee))
                .settlementAmount(Money.of(settlement))
                .build();
    }
}
