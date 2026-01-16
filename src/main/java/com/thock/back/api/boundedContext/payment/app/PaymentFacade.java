package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import com.thock.back.api.boundedContext.payment.domain.Wallet;
import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final PaymentSyncMemberUseCase paymentSyncMemberUseCase;
    private final PaymentSupport paymentSupport;

    @Transactional
    public PaymentMember syncMember(MemberDto member){
        return paymentSyncMemberUseCase.syncMember(member);
    }

    @Transactional(readOnly = true)
    public Optional<Wallet> findWalletByHolder(PaymentMember holder) {
        return paymentSupport.findWalletByHolder(holder);
    }

}
