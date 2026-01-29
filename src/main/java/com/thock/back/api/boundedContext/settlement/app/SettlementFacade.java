package com.thock.back.api.boundedContext.settlement.app;

import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service; // Facade도 Service입니다.
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementFacade {

    private final DailySettlementUseCase dailySettlementUseCase;
    private final SettlementMemberSyncUseCase settlementMemberSyncUseCase;
    // 추후: private final SettlementQueryUseCase queryUseCase; (조회용)

    /**
     * [스케줄러용] 일일 정산 프로세스 실행
     */
    public void runDailySettlement(LocalDate date) {
        log.info("Facade: 일일 정산 시작 요청 - date: {}", date);
        dailySettlementUseCase.executeProcess(date);
        log.info("Facade: 일일 정산 종료");
    }

    /**
     * [리스너용] 판매자 정보 동기화
     * 이벤트 리스너도 UseCase를 직접 알 필요 없이 Facade에게 위임.
     */
    public void syncMember(MemberDto memberDto) {
        settlementMemberSyncUseCase.syncMember(memberDto);
    }

    /**
     * [컨트롤러용 - 미래 대비]
     * 나중에 프론트에서 "내 정산 내역 보여줘" 하면 여기서 조회 로직을 연결함.
     * public List<SettlementDto> getSettlementHistory(Long memberId) { ... }
     */
}