package com.thock.back.api.boundedContext.settlement.in;

import com.thock.back.api.boundedContext.settlement.app.SettlementFacade;
import com.thock.back.api.boundedContext.settlement.out.SettlementMemberRepository;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.dto.MemberDto;
import com.thock.back.api.shared.member.event.MemberModifiedEvent;
import com.thock.back.api.shared.member.event.SellerRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SettlementEventListener {
    private final SettlementFacade settlementFacade;
//TODO:
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(MemberModifiedEvent event) {
        MemberDto memberDto = event.getMember();

        // [필터링] "판매자(SELLER)"인 경우에만 정산 모듈로 정보를 가져옵니다.
        if (memberDto.getRole() == MemberRole.SELLER) {
            settlementFacade.syncMember(memberDto);
        }
    }
}
