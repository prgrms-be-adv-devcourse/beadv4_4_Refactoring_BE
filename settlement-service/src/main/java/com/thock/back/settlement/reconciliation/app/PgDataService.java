package com.thock.back.settlement.reconciliation.app;

import com.thock.back.settlement.reconciliation.domain.PgSalesRaw;
import com.thock.back.settlement.reconciliation.in.dto.PgSalesDto;
import com.thock.back.settlement.reconciliation.out.PgDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PgDataService {

    private final PgDataRepository pgDataRepository;

    // dto에 이미 생성자로 만드는 로직을 저장해뒀기에, 여기서는 그걸 사용 후 데이터 저장만
    @Transactional
    public void saveAllPgData(List<PgSalesDto> dtos){
        List<PgSalesRaw> entities = dtos.stream()
                .map(PgSalesDto::toEntity)
                .toList();
        pgDataRepository.saveAll(entities);
    }
}
