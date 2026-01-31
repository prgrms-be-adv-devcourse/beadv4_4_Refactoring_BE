package com.thock.back.member.out;


import com.thock.back.member.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findAllByMemberIdAndRevokedAtIsNull(Long memberId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.memberId = :memberId AND rt.revokedAt IS NULL")
    int revokeAllByMemberId(@Param("memberId") Long memberId, @Param("revokedAt")LocalDateTime revokedAt);
}
