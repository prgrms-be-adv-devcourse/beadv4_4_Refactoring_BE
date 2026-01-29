package com.thock.back.api.boundedContext.payment.in;

import com.thock.back.api.boundedContext.payment.app.PaymentConfirmService;
import com.thock.back.api.boundedContext.payment.app.PaymentFacade;
import com.thock.back.api.boundedContext.payment.domain.dto.request.PaymentConfirmRequestDto;
import com.thock.back.api.boundedContext.payment.domain.dto.response.PaymentLogResponseDto;
import com.thock.back.api.boundedContext.payment.domain.dto.response.RevenueLogResponseDto;
import com.thock.back.api.boundedContext.payment.domain.dto.response.WalletLogResponseDto;
import com.thock.back.api.global.security.AuthContext;
import com.thock.back.api.shared.payment.dto.WalletDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "payment-controller", description = "결제, 지갑 관련 API(지갑 조회, 로그 조회, 결제 내역 조회 등등)")
public class ApiV1PaymentController {
    private final PaymentConfirmService paymentConfirmService;
    private final PaymentFacade paymentFacade;

    @Operation(
            summary = "지갑 조회",
            description = "사용자의 지갑을 조회합니다." +
                    "지갑 정보를 반환합니다..")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지갑 조회 성공"),
            @ApiResponse(responseCode = "WALLET-404-1", description = "지갑을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "WALLET-404-2", description = "이 지갑은 현재 정지 된 상태입니다.")
    })
    @GetMapping("internal/wallets/{memberId}")
    public ResponseEntity<WalletDto> getInternalWallet(@PathVariable("memberId") Long memberId) {
        // 내부 호출용 API
        return ResponseEntity.ok().body(paymentFacade.walletFindByMemberId(memberId));
    }

    @Operation(
            summary = "지갑 잔액 입출금 로그 조회",
            description = "사용자의 지갑 잔액 입출금 로그를 조회합니다." +
                    "지갑 잔액 입출금 로그를 반환합니다..")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지갑 조회 성공"),
            @ApiResponse(responseCode = "WALLET-404-1", description = "지갑을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "WALLET-404-2", description = "이 지갑은 현재 정지 된 상태입니다.")
    })
    @GetMapping("/balanceLog")
    public ResponseEntity<WalletLogResponseDto> getWalletLog() throws Exception {
        Long memberId = AuthContext.memberId();
        return ResponseEntity.ok().body(paymentFacade.getWalletLog(memberId));
    }

    @Operation(
            summary = "결제 내역 로그 조회",
            description = "사용자의 결제 내역 로그를 조회합니다." +
                    "결제 내역 로그를 반환합니다..")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지갑 조회 성공"),
            @ApiResponse(responseCode = "WALLET-404-1", description = "지갑을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "WALLET-404-2", description = "이 지갑은 현재 정지 된 상태입니다.")
    })
    @GetMapping("/paymentLog")
    public ResponseEntity<PaymentLogResponseDto> getPaymentLog() throws Exception {
        Long memberId = AuthContext.memberId();
        return ResponseEntity.ok().body(paymentFacade.getPaymentLog(memberId));
    }

    @Operation(
            summary = "지갑 판매수익 입출금 로그 조회",
            description = "사용자의 지갑 판매수익 입출금 로그를 조회합니다." +
                    "지갑 판매수익 입출금 로그를 반환합니다..")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지갑 조회 성공"),
            @ApiResponse(responseCode = "WALLET-404-1", description = "지갑을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "WALLET-404-2", description = "이 지갑은 현재 정지 된 상태입니다.")
    })
    @GetMapping("/revenueLog")
    public ResponseEntity<RevenueLogResponseDto> getRevenueLog() throws Exception {
        Long memberId = AuthContext.memberId();
        return ResponseEntity.ok().body(paymentFacade.getRevenueLog(memberId));
    }

    @Operation(
            summary = "결제 검증",
            description = "토스페이먼츠에서 결제 요청한 금액과 실제 결제가 된 금액을 비교 검증합니다.. " +
                    "결제 검증 결과를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 검증 성공"),
            @ApiResponse(responseCode = "PAYMENT-404-1", description = "주문번호에 맞는 결제정보가 없습니다."),
            @ApiResponse(responseCode = "PAYMENT-400-9", description = "결제 상태가 요청이 아닙니다.")
    })

    @PostMapping("/confirm/toss")
    public ResponseEntity<?> confirmToss(@RequestBody PaymentConfirmRequestDto request) {
        return ResponseEntity.ok(paymentConfirmService.confirmPayment(request));
    }

//    @Operation(
//            summary = "결제 취소",
//            description = "토스페이먼츠에서 결제(전액) 완료된 주문를 취소합니다. " +
//                    "결제 취소 결과를 반환합니다.")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "결제 취소 성공"),
//            @ApiResponse(responseCode = "PAYMENT-404-1", description = "주문번호에 맞는 결제정보가 없습니다."),
//            @ApiResponse(responseCode = "PAYMENT-400-10", description = "결제 상태가 완료가 아닙니다."),
//            @ApiResponse(responseCode = "PAYMENT-400-11", description = "요청 멤버하고 결제 멤버하고 다릅니다."),
//            @ApiResponse(responseCode = "REFUND-404-1", description = "환불 사유가 비어있습니다.")
//    })
//
//    @PostMapping("/cancel/toss")
//    public ResponseEntity<?> cancelToss(@RequestBody PaymentCancelRequestDto request) throws Exception {
//        Long memberId = AuthContext.memberId();
//        return ResponseEntity.ok(paymentConfirmService.cancelToss(request, memberId));
//    }
//
//    @Operation(
//            summary = "결제 취소",
//            description = "결제(전액) 완료된 주문를 취소합니다. " +
//                    "결제 취소 결과를 반환합니다.")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "결제 취소 성공"),
//            @ApiResponse(responseCode = "PAYMENT-404-1", description = "주문번호에 맞는 결제정보가 없습니다."),
//            @ApiResponse(responseCode = "PAYMENT-400-10", description = "결제 상태가 완료가 아닙니다."),
//            @ApiResponse(responseCode = "PAYMENT-400-11", description = "요청 멤버하고 결제 멤버하고 다릅니다."),
//            @ApiResponse(responseCode = "REFUND-404-1", description = "환불 사유가 비어있습니다.")
//    })
//
//    @PostMapping("/cancel")
//    public ResponseEntity<RefundResponseDto> cancelPayment(@RequestBody PaymentCancelRequestDto request) throws Exception {
//        Long memberId = AuthContext.memberId();
//        return ResponseEntity.ok(paymentConfirmService.cancelPayment(request, memberId));
//    }
}
