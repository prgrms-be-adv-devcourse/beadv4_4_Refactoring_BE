package com.thock.back.api.boundedContext.payment.in;

import com.thock.back.api.boundedContext.market.in.dto.res.CartItemListResponse;
import com.thock.back.api.boundedContext.payment.app.PaymentConfirmService;
import com.thock.back.api.boundedContext.payment.domain.dto.PaymentConfirmRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "payment-controller", description = "결제, 지갑 관련 API(지갑 조회, 로그 조회, 결제 내역 조회 등등)")
public class ApiV1PaymentController {
    private final PaymentConfirmService paymentConfirmService;
    //TODO: 지갑 조회 API

    //TODO: Wallet Log View API

    //TODO: Payment Log view API

    //TODO: Revenue Log View API

    @Operation(
            summary = "결제 검증",
            description = "토스페이먼츠에서 결제 요청한 금액과 실제 결제가 된 금액을 비교 검증합니다.. " +
                    "결제 검증 결과를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 검증 성공")
    })

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody PaymentConfirmRequestDto request) {
        return ResponseEntity.ok(paymentConfirmService.confirmPayment(request));
    }
}
