package com.thock.back.api.boundedContext.payment.app;

import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final PaymentSyncMemberUseCase paymentSyncMemberUseCase;

    @Transactional
    public PaymentMember syncMember(MemberDto member){
        return paymentSyncMemberUseCase.syncMember(member);
    }

}
