package com.thock.back.payment.in;

import com.thock.back.global.security.AuthUser;
import com.thock.back.global.security.AuthenticatedUser;
import com.thock.back.payment.app.PaymentAccountWithdrawUseCase;
import com.thock.back.payment.app.PaymentConfirmAndRefundUseCase;
import com.thock.back.payment.app.PaymentFacade;
import com.thock.back.payment.domain.dto.request.PaymentConfirmRequestDto;
import com.thock.back.payment.domain.dto.response.PaymentLogResponseDto;
import com.thock.back.payment.domain.dto.response.RevenueLogResponseDto;
import com.thock.back.payment.domain.dto.response.WalletLogResponseDto;
import com.thock.back.shared.payment.dto.WalletDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "payment-controller", description = "결제, 지갑 관련 API(지갑 조회, 로그 조회, 결제 내역 조회 등등)")
public class ApiV1PaymentController {
    private final PaymentFacade paymentFacade;

    @Operation(
            summary = "지갑 조회 (내부 API)",
            description = "사용자의 지갑을 조회합니다. 지갑 정보를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지갑 조회 성공"),
            @ApiResponse(responseCode = "404", description = "WALLET-404-1: 지갑을 찾을 수 없습니다. / WALLET-404-2: 이 지갑은 현재 정지 된 상태입니다.")
    })
    @GetMapping("internal/wallets/{memberId}")
    public ResponseEntity<WalletDto> getInternalWallet(@PathVariable("memberId") Long memberId) {
        // 내부 호출용 API
        log.info("Payment API : getInternalWallet / memberId = {}", memberId);
        return ResponseEntity.ok().body(paymentFacade.walletFindByMemberId(memberId));
    }

    @Operation(
            summary = "지갑 조회 (외부 API)",
            description = "현재 로그인한 사용자의 지갑 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지갑 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "WALLET-404-1: 지갑을 찾을 수 없습니다. / WALLET-404-2: 이 지갑은 현재 정지 된 상태입니다.")
    })
    @GetMapping("/wallet/me")
    public ResponseEntity<WalletDto> getMyWallet(@AuthUser AuthenticatedUser user) {
        Long memberId = user.memberId();
        log.info("Payment API : getMyWallet / memberId = {}", memberId);
        return ResponseEntity.ok().body(paymentFacade.walletFindByMemberId(memberId));
    }

    @Operation(
            summary = "지갑 잔액 입출금 로그 조회",
            description = "사용자의 지갑 잔액 입출금 로그를 조회합니다. 지갑 잔액 입출금 로그를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "잔액 로그 조회 성공"),
            @ApiResponse(responseCode = "404", description = "WALLET-404-1: 지갑을 찾을 수 없습니다. / WALLET-404-2: 이 지갑은 현재 정지 된 상태입니다.")
    })
    @GetMapping("/balanceLog")
    public ResponseEntity<WalletLogResponseDto> getWalletLog(@AuthUser AuthenticatedUser user){
        Long memberId = user.memberId();
        log.info("Payment API : getWalletLog / memberId = {}", memberId);
        return ResponseEntity.ok().body(paymentFacade.getWalletLog(memberId));
    }

    @Operation(
            summary = "결제 내역 로그 조회",
            description = "사용자의 결제 내역 로그를 조회합니다. 결제 내역 로그를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 로그 조회 성공")
    })
    @GetMapping("/paymentLog")
    public ResponseEntity<PaymentLogResponseDto> getPaymentLog(@AuthUser AuthenticatedUser user){
        Long memberId = user.memberId();
        log.info("Payment API : getPaymentLog / memberId = {}", memberId);
        return ResponseEntity.ok().body(paymentFacade.getPaymentLog(memberId));
    }

    @Operation(
            summary = "지갑 판매수익 입출금 로그 조회",
            description = "사용자의 지갑 판매수익 입출금 로그를 조회합니다. 지갑 판매수익 입출금 로그를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "판매수익 로그 조회 성공"),
            @ApiResponse(responseCode = "404", description = "WALLET-404-1: 지갑을 찾을 수 없습니다. / WALLET-404-2: 이 지갑은 현재 정지 된 상태입니다.")
    })
    @GetMapping("/revenueLog")
    public ResponseEntity<RevenueLogResponseDto> getRevenueLog(@AuthUser AuthenticatedUser user) {
        Long memberId = user.memberId();
        log.info("Payment API : getRevenueLog / memberId = {}", memberId);
        return ResponseEntity.ok().body(paymentFacade.getRevenueLog(memberId));
    }

    @Operation(
            summary = "결제 검증",
            description = "토스페이먼츠에서 결제 요청한 금액과 실제 결제가 된 금액을 비교 검증합니다. 결제 검증 결과를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 검증 성공"),
            @ApiResponse(responseCode = "400", description = "PAYMENT-400-9: 결제 상태가 요청이 아닙니다. / PAYMENT-400-4: 토스 결제 승인 실패 / PAYMENT-400-6: 결제 금액이 일치하지 않습니다."),
            @ApiResponse(responseCode = "404", description = "PAYMENT-404-1: 주문번호에 맞는 결제정보가 없습니다. / MEMBER-404-1: 회원을 찾을 수 없습니다. / WALLET-404-1: 지갑을 찾을 수 없습니다.")
    })
    @PostMapping("/confirm/toss")
    public ResponseEntity<?> confirmToss(@RequestBody PaymentConfirmRequestDto request) {
        log.info("Payment API : confirmToss / orderId = {}", request.getOrderId());
        return ResponseEntity.ok(paymentFacade.confirmPayment(request));
    }

    @Operation(
            summary = "계좌 출금 신청",
            description = "판매 수익을 계좌로 출금 신청합니다. 출금 결과 메시지를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "출금 신청 성공"),
            @ApiResponse(responseCode = "400", description = "PAYMENT-400-12: 출금 금액이 올바르지 않습니다. / GLOBAL-2: 해당 유저의 등급이 판매자가 아닙니다."),
            @ApiResponse(responseCode = "404", description = "MEMBER-404-1: 회원을 찾을 수 없습니다. / WALLET-404-1: 지갑을 찾을 수 없습니다.")
    })
    @PostMapping("/withdraw")
    public ResponseEntity   <?> accountWithdraw(@RequestBody Long amount, @AuthUser AuthenticatedUser user) {
        Long memberId = user.memberId();
        log.info("Payment API : accountWithdraw / memberId = {}", memberId);
        return ResponseEntity.ok().body(paymentFacade.accountWithdraw(memberId, amount));
    }
}
