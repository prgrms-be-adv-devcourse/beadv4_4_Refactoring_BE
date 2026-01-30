package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.PaymentLog;
import com.thock.back.api.boundedContext.payment.domain.RevenueLog;
import com.thock.back.api.boundedContext.payment.domain.Wallet;
import com.thock.back.api.boundedContext.payment.domain.WalletLog;
import com.thock.back.api.boundedContext.payment.domain.dto.response.PaymentLogResponseDto;
import com.thock.back.api.boundedContext.payment.domain.dto.response.RevenueLogResponseDto;
import com.thock.back.api.boundedContext.payment.domain.dto.response.WalletLogResponseDto;
import com.thock.back.api.boundedContext.payment.out.*;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.payment.dto.WalletDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentFindUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentMemberRepository paymentMemberRepository;
    private final WalletRepository walletRepository;
    private final WalletLogRepository walletLogRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final RevenueLogRepository  revenueLogRepository;

    public WalletDto walletFindByMemberId(Long memberId) {
        Wallet wallet = walletRepository.findByHolderId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));

        if(wallet.getHolder().getState() == MemberState.INACTIVE){
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }

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
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));

        if(wallet.getHolder().getState() == MemberState.INACTIVE){
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }

        List<WalletLog> logs = walletLogRepository.findByWalletId(wallet.getId());

        return new WalletLogResponseDto(
                memberId,
                wallet.getId(),
                logs
        );
    }

    public RevenueLogResponseDto getRevenueLog(Long memberId) {
        Wallet wallet = walletRepository.findByHolderId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));

        if(wallet.getHolder().getState() == MemberState.INACTIVE){
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }

        List<RevenueLog> logs = revenueLogRepository.findByWalletId(wallet.getId());

        return new RevenueLogResponseDto(
                memberId,
                wallet.getId(),
                logs
        );
    }

    public PaymentLogResponseDto getPaymentLog(Long memberId) {
        List<PaymentLog> logs = paymentLogRepository.findByBuyerId(memberId);

        return new PaymentLogResponseDto(
                memberId,
                logs
        );
    }
}
