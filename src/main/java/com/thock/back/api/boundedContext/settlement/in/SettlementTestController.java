package com.thock.back.api.boundedContext.settlement.in;

import com.thock.back.api.boundedContext.settlement.app.DailySettlementScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SettlementTestController {

    private final DailySettlementScheduler scheduler;

    // 브라우저에서 http://localhost:8080/test/run 입력하면 실행됨
    @GetMapping("/test/run")
    public String forceRun() {
        scheduler.runDailySettlement(); // 스케줄러 강제 실행!
        return "정산 배치 강제 실행 완료! H2 Console을 확인하세요.";
    }
}