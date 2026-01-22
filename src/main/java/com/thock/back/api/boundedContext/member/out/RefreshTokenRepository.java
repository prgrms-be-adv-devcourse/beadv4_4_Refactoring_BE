package com.thock.back.api.boundedContext.member.out;

import com.thock.back.api.boundedContext.member.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenValue(String tokenValue);
    List<RefreshToken> findAllByMemberIdAndRevokedAtIsNull(Long memberId);
}
