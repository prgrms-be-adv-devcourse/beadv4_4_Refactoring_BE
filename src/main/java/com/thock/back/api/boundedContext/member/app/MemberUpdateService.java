package com.thock.back.api.boundedContext.member.app;

import com.thock.back.api.boundedContext.member.domain.entity.Member;
import com.thock.back.api.boundedContext.member.out.MemberRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.event.MemberModifiedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberUpdateService {

    private final MemberRepository memberRepository;
    private final EventPublisher eventPublisher;

    public void updateMemberRole(Long memberId, String bankCode, String accountNumber, String accountHolder) throws Exception {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String memberRole = member.getRole().toString();

        if (memberRole.equals("USER")) {
            member.setRole(MemberRole.SELLER);
            member.setBankCode(bankCode);
            member.setAccountNumber(accountNumber);
            member.setAccountHolder(accountHolder);
            memberRepository.save(member);

            eventPublisher.publish(new MemberModifiedEvent(member.toDto()));
        } else {
            throw new Exception();
        }
    }
}
