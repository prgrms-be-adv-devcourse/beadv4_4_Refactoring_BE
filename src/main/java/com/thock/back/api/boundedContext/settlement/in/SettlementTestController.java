package com.thock.back.api.boundedContext.settlement.in;

import com.thock.back.api.boundedContext.settlement.app.DailySettlementScheduler;
import com.thock.back.api.boundedContext.settlement.app.SettlementFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class SettlementTestController {
    private final SettlementFacade settlementFacade;     // ⭕ 파사드를 직접 써야 날짜를 내 맘대로 넣음

    // 브라우저에서 http://localhost:8080/test/run 입력하면 실행됨
    @GetMapping("/test/run")
    public String forceRun(@RequestParam(required = false) LocalDate date) {

        // 날짜 파라미터가 없으면 '오늘' 날짜로 실행! (방금 주문한 거 테스트 가능)
        if (date == null) {
            date = LocalDate.now();
        }

        settlementFacade.runDailySettlement(date); // 👈 파사드에게 "오늘 날짜"를 줌

        return date + " 일자 정산 배치 강제 실행 완료! H2 Console을 확인하세요.";
    }
}