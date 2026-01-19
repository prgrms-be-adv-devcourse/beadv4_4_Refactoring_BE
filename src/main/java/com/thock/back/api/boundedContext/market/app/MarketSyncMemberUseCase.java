package com.thock.back.api.boundedContext.market.app;

import com.thock.back.api.boundedContext.market.domain.MarketMember;
import com.thock.back.api.boundedContext.market.out.MarketMemberRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.shared.market.event.MarketMemberCreatedEvent;
import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketSyncMemberUseCase {
    private final MarketMemberRepository marketMemberRepository;
    private final EventPublisher eventPublisher;
    public MarketMember syncMember(MemberDto member) {
        // 기존 회원인지 판단
        boolean isNew = !marketMemberRepository.existsById(member.getId());

        MarketMember _member = marketMemberRepository.save(
                new MarketMember(
                        member.getEmail(),
                        member.getName(),
                        member.getRole(),
                        member.getState(),
                        member.getId(),
                        member.getCreatedAt(),
                        member.getUpdatedAt()
                )
        );

        // MarketMember가 처음 생성 될 때 Cart도 생성시킴
        if (isNew) {
            eventPublisher.publish(
                    new MarketMemberCreatedEvent(_member.toDto())
            );
        }
        return _member;
    }
}
