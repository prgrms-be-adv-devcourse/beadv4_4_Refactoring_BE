package com.thock.back.api.boundedContext.member.app;

import com.thock.back.api.boundedContext.member.out.MemberRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberUpdateService {

    private final MemberRepository memberRepository;
    private final EventPublisher eventPublisher;


}
