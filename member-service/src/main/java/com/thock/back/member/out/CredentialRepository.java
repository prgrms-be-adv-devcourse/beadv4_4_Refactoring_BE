package com.thock.back.member.out;


import com.thock.back.member.domain.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByMemberId(Long id);
}
