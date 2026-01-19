package com.thock.back.api.boundedContext.member.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "member_refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_member_id", columnList = "member_id"),
                @Index(name = "idx_refresh_token_value", columnList = "token_value", unique = true)
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "token_value", nullable = false, length = 200, unique = true)
    private String tokenValue;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private RefreshToken(Long memberId, String tokenValue, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.tokenValue = tokenValue;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(Long memberId, String tokenValue, LocalDateTime expiresAt) {
        return new RefreshToken(memberId, tokenValue, expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }
}
