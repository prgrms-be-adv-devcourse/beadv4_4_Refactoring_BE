package com.thock.back.settlement.reconciliation.in.controller;

import com.thock.back.settlement.reconciliation.app.ReconciliationFacade;
import com.thock.back.settlement.reconciliation.in.dto.PgSalesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PgDataController {
    private final ReconciliationFacade reconciliationFacade;

    // pg데이터 받는 컨트롤러
    @PostMapping("/api/v1/settlements/reconciliation/pg-data")
    public void uploadPgData(@RequestBody List<PgSalesDto> dtos){
        reconciliationFacade.receivePgData(dtos);
    }
}
