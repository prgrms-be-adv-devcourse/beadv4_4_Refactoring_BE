package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import com.thock.back.api.boundedContext.payment.domain.Wallet;
import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
import com.thock.back.api.boundedContext.payment.out.WalletRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentSyncMemberUseCase {
    private final PaymentMemberRepository paymentMemberRepository;
    private final WalletRepository walletRepository;
    private final EventPublisher eventPublisher;

    /**
     *     Member - PaymentMember 동기화
     **/

    public PaymentMember syncMember(MemberDto member){
        boolean isNew = !paymentMemberRepository.existsById(member.getId());

        PaymentMember paymentMember = paymentMemberRepository.save(
                new PaymentMember(
                        member.getEmail(),
                        member.getName(),
                        member.getState(),
                        member.getRole(),
                        member.getId(),
                        member.getCreatedAt(),
                        member.getUpdatedAt()
                )
        );

        if(isNew){
            Wallet wallet = new Wallet(paymentMember);
            walletRepository.save(wallet);
        }
        return paymentMember;
    }
}
