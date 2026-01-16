package com.thock.back.api.boundedContext.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByMemberId(Long memberId);
}
