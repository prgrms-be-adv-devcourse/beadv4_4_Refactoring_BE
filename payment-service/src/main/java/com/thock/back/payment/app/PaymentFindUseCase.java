package com.thock.back.payment.app;


import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.payment.domain.PaymentLog;
import com.thock.back.payment.domain.RevenueLog;
import com.thock.back.payment.domain.Wallet;
import com.thock.back.payment.domain.WalletLog;
import com.thock.back.payment.domain.dto.response.*;
import com.thock.back.payment.out.*;
import com.thock.back.shared.member.domain.MemberState;
import com.thock.back.shared.payment.dto.WalletDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFindUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentMemberRepository paymentMemberRepository;
    private final WalletRepository walletRepository;
    private final WalletLogRepository walletLogRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final RevenueLogRepository  revenueLogRepository;

    public WalletDto walletFindByMemberId(Long memberId) {
        Wallet wallet = walletRepository.findByHolderId(memberId)
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - memberId={}", memberId);
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });

        if (wallet.getHolder().getState() == MemberState.INACTIVE) {
            log.error("비활성화된 지갑 접근 시도 - memberId={}", memberId);
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }

        log.info("지갑 조회 완료 - memberId={}", memberId);
        return new WalletDto(
                wallet.getId(),
                wallet.getHolder().getId(),
                wallet.getHolder().getName(),
                wallet.getBalance(),
                wallet.getRevenue(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    public WalletLogResponseDto getWalletLog(Long memberId) {
        Wallet wallet = walletRepository.findByHolderId(memberId)
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - memberId={}", memberId);
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });

        if (wallet.getHolder().getState() == MemberState.INACTIVE) {
            log.error("비활성화된 지갑 접근 시도 - memberId={}", memberId);
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }

        List<WalletLog> logs = walletLogRepository.findByWalletId(wallet.getId());
        List<WalletLogItemDto> walletLogs = logs.stream()
                .map(logItem -> new WalletLogItemDto(
                        logItem.getId(),
                        logItem.getEventType(),
                        logItem.getAmount(),
                        logItem.getBalance(),
                        logItem.getCreatedAt()
                ))
                .collect(Collectors.toList());

        log.info("지갑 로그 조회 완료 - memberId={}, logCount={}", memberId, logs.size());
        return new WalletLogResponseDto(
                memberId,
                wallet.getId(),
                walletLogs
        );
    }

    public RevenueLogResponseDto getRevenueLog(Long memberId) {
        Wallet wallet = walletRepository.findByHolderId(memberId)
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - memberId={}", memberId);
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });

        if (wallet.getHolder().getState() == MemberState.INACTIVE) {
            log.error("비활성화된 지갑 접근 시도 - memberId={}", memberId);
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }

        List<RevenueLog> logs = revenueLogRepository.findByWalletId(wallet.getId());
        List<RevenueLogItemDto> revenueLogs = logs.stream()
                .map(logItem -> new RevenueLogItemDto(
                        logItem.getId(),
                        logItem.getEventType(),
                        logItem.getAmount(),
                        logItem.getBalance(),
                        logItem.getCreatedAt()
                ))
                .collect(Collectors.toList());

        log.info("수익 로그 조회 완료 - memberId={}, logCount={}", memberId, logs.size());
        return new RevenueLogResponseDto(
                memberId,
                wallet.getId(),
                revenueLogs
        );
    }

    public PaymentLogResponseDto getPaymentLog(Long memberId) {
        List<PaymentLog> logs = paymentLogRepository.findByBuyerId(memberId);
        List<PaymentLogItemDto> paymentLogs = logs.stream()
                .map(logItem -> new PaymentLogItemDto(
                        logItem.getId(),
                        logItem.getOrderId(),
                        logItem.getPaymentStatus(),
                        logItem.getAmount(),
                        logItem.getPgAmount(),
                        logItem.getRefundedAmount(),
                        logItem.getCreatedAt()
                ))
                .collect(Collectors.toList());

        log.info("결제 로그 조회 완료 - memberId={}, logCount={}", memberId, logs.size());
        return new PaymentLogResponseDto(
                memberId,
                paymentLogs
        );
    }
}
