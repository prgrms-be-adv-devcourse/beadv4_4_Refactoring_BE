package com.thock.back.settlement.shared.reconciliation.port;


import com.thock.back.settlement.shared.reconciliation.dto.SettlementTargetDto;

import java.util.List;

// 대사 끝난 애들 중 정산 대상 데이터 가져오도록 인터페이스 구현
public interface SettlementTargetProvider {
    List<SettlementTargetDto> getReadySnapshots(Long id, int size);
}
