package com.thock.back.settlement.reconciliation.app.useCase;

import com.thock.back.settlement.reconciliation.domain.PgSalesRaw;
import com.thock.back.settlement.reconciliation.in.dto.PgSalesDto;
import com.thock.back.settlement.reconciliation.out.PgSalesRawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavePgDataUseCase {
    private final PgSalesRawRepository pgSalesRawRepository;

    @Transactional
    public void execute(List<PgSalesDto> dtos){
        List<PgSalesRaw> entities = dtos.stream()
                .map(PgSalesDto::toEntity)
                .toList();
        pgSalesRawRepository.saveAll(entities);
    }
}
