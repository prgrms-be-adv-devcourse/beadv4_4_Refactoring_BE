package com.thock.back.member.app;


import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.member.domain.command.PromoteToSellerCommand;
import com.thock.back.member.domain.entity.Member;
import com.thock.back.member.out.MemberRepository;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.event.MemberModifiedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberPromoteService {

    private final MemberRepository memberRepository;
    private final EventPublisher eventPublisher;

    public void promoteToSeller(PromoteToSellerCommand command) {
        Member member = memberRepository.findById(command.memberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        member.promoteToSeller(command.bankCode(), command.accountNumber(), command.accountHolder());
        memberRepository.save(member);

        eventPublisher.publish(new MemberModifiedEvent(member.toDto()));
    }
}
