package com.thock.back.api.boundedContext.settlement.in;

import com.thock.back.api.boundedContext.settlement.app.SettlementFacade;
import com.thock.back.api.boundedContext.settlement.out.SettlementMemberRepository;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.dto.MemberDto;
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
    public void handle(SellerRegisteredEvent event) {
        MemberDto memberDto = MemberDto.builder()
                .id(event.getMemberId())          // ID 매핑
                .email(event.getEmail())
                .name(event.getName())
                // 이벤트는 '판매자 등록'이니까 권한과 상태를 확실하게 박아줍니다.
                .role(MemberRole.SELLER)
                .state(MemberState.ACTIVE)
                .bankCode(event.getBankCode())
                .accountNumber(event.getAccountNumber())
                .accountHolder(event.getAccountHolder())
                .createdAt(event.getCreatedAt())  // 원본 가입일
                .updatedAt(event.getUpdatedAt())  // 원본 수정일
                .build();

        // 2. Facade 호출
        settlementFacade.syncMember(memberDto);
    };
}
