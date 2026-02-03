package com.thock.back.settlement.reconciliation.in.controller;

import com.thock.back.settlement.reconciliation.app.PgDataService;
import com.thock.back.settlement.reconciliation.in.dto.PgSalesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PgDataController {
    private final PgDataService pgDataService;

    @PostMapping("/api/v1/finance/reconciliation/pg-data")
    public void uploadPgData(@RequestBody List<PgSalesDto> dtos){
        pgDataService.saveAllPgData(dtos);
    }
}
