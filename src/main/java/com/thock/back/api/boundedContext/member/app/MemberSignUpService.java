package com.thock.back.api.boundedContext.member.app;

import com.thock.back.api.boundedContext.member.domain.Credential;
import com.thock.back.api.boundedContext.member.domain.Member;
import com.thock.back.api.boundedContext.member.domain.SignUpCommand;
import com.thock.back.api.boundedContext.member.out.CredentialRepository;
import com.thock.back.api.boundedContext.member.out.MemberRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.shared.member.event.MemberJoinedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberSignUpService {

    private final MemberRepository memberRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    public Long signUp(SignUpCommand command) {

        if (memberRepository.existsByEmail(command.email())) {
            throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // Member 생성
        Member member = Member.signUp(command.email(), command.name());
        memberRepository.save(member);

        // 비밀번호 해싱 후 Credential 생성
        String hashedPassword = passwordEncoder.encode(command.password());
        Credential credential = Credential.create(member.getId(), hashedPassword);
        credentialRepository.save(credential);


        eventPublisher.publish(new MemberJoinedEvent(member.toDto()));

        return member.getId();
    }
}
