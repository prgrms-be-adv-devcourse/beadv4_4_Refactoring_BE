package com.thock.back.member.app;


import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.member.domain.command.SignUpCommand;
import com.thock.back.member.domain.entity.Credential;
import com.thock.back.member.domain.entity.Member;
import com.thock.back.member.out.CredentialRepository;
import com.thock.back.member.out.MemberRepository;
import com.thock.back.shared.member.event.MemberJoinedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            throw new CustomException(ErrorCode.MEMBER_EMAIL_ALREADY_EXISTS);
        }

        // Member 생성
        Member member = Member.signUp(command.email(), command.name());
        Member savedMember = memberRepository.save(member);

        // 비밀번호 해싱 후 Credential 생성
        String hashedPassword = passwordEncoder.encode(command.password());
        Credential credential = Credential.create(savedMember.getId(), hashedPassword);
        credentialRepository.save(credential);


        eventPublisher.publish(new MemberJoinedEvent(savedMember.toDto()));

        return member.getId();
    }
}
