package com.thock.back.api.boundedContext.member.domain;

import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
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

    public Long signUp(SignUpCommand command) {

        if (memberRepository.existsByEmail(command.email())) {
            throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // Member 생성
        Member member = Member.signUp(command.email(), command.name());
        memberRepository.save(member);

        // 비밀번호 해싱 후 Credential 생성
        String hashedPassword = passwordEncoder.encode(command.password());
        Credential credential = Credential.create(member, hashedPassword);
        credentialRepository.save(credential);

        return member.getId();
    }
}
