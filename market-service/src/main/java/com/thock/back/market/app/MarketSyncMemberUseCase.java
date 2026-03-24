package com.thock.back.market.app;

import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.shared.market.event.MarketMemberCreatedEvent;
import com.thock.back.shared.member.dto.MemberDto;
import com.thock.back.market.domain.MarketMember;
import com.thock.back.market.out.repository.MarketMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketSyncMemberUseCase {
    private final MarketMemberRepository marketMemberRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void syncMember(MemberDto member) {
        // 기존 회원인지 판단
        boolean isNew = !marketMemberRepository.existsById(member.id());

        MarketMember _member = marketMemberRepository.save(
                new MarketMember(
                        member.email(),
                        member.name(),
                        member.role(),
                        member.state(),
                        member.id(),
                        member.createdAt(),
                        member.updatedAt()
                )
        );

        // MarketMember가 처음 생성 될 때 Cart도 생성시킴
        if (isNew) {
            eventPublisher.publish(
                    new MarketMemberCreatedEvent(_member.toDto())
            );
        }
    }
}
