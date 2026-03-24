package com.thock.back.payment.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.payment.domain.EventType;
import com.thock.back.payment.domain.Payment;
import com.thock.back.payment.domain.PaymentMember;
import com.thock.back.payment.domain.Wallet;
import com.thock.back.payment.out.PaymentMemberRepository;
import com.thock.back.payment.out.WalletRepository;
import com.thock.back.shared.common.dto.DefaultResponseDto;
import com.thock.back.shared.member.domain.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAccountWithdrawUseCase {
    private final WalletRepository walletRepository;
    private final PaymentMemberRepository paymentMemberRepository;
    private static final Long MIN_WITHDRAW_AMOUNT = 1000L;

    @Transactional
    // 실제 계좌출금 API 구축 시 saga패턴 적용 필요
    public DefaultResponseDto accountWithdraw(Long memberId, Long amount) {
        // 멤버 정상 판단
        PaymentMember member = paymentMemberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.error("멤버 조회 실패 - memberId={}", memberId);
                    return new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                });

        // 지갑 상태 판단
        Wallet wallet = walletRepository.findByHolderId(member.getId())
                .orElseThrow(() -> {
                    log.error("지갑 조회 실패 - memberId={}", member.getId());
                    return new CustomException(ErrorCode.WALLET_NOT_FOUND);
                });
        // 출금 금액 정상 판단
        if (amount < MIN_WITHDRAW_AMOUNT || amount <= 0 || wallet.getRevenue() < amount) {
            log.error("출금 금액 올바르지 않음 - memberId={}", memberId);
            throw new CustomException(ErrorCode.INVALID_AMOUNT);
        }

        // 멤버 Role 판단
        if (member.getRole() != MemberRole.SELLER) {
            log.error("멤버가 등급이 판매자가 아님 memberId={}", memberId);
            throw new CustomException(ErrorCode.INVALID_ROLE_SELLER);
        }

        wallet.withdrawRevenue(amount);
        walletRepository.save(wallet);

        wallet.createRevenueLogEvent(amount, EventType.출금);

        // TODO: 실제 은행 API 호출 후 출금 진행 (사업자번호가 없어서 실제로 구현은 불가능)

        String body = String.format("%,d원 출금 신청이 완료되었습니다.", amount);
        DefaultResponseDto responseDto = new DefaultResponseDto(body);
        return responseDto;
    }
}
