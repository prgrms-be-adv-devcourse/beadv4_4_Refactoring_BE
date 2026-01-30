package com.thock.back.api.global.security;

import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    /** AccessToken 생성 (짧게) */
    public String createAccessToken(Long memberId, MemberRole role, MemberState state) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTokenExpSeconds());

        return Jwts.builder()
                .issuer(props.issuer())
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("role", role.name())
                .claim("state", state.name())
                .signWith(key)
                .compact();
    }

    /** RefreshToken 생성 (길게) */
    public String createRefreshToken(Long memberId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.refreshTokenExpSeconds());

        return Jwts.builder()
                .issuer(props.issuer())
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long extractMemberId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public MemberRole extractRole(String token) {
        return MemberRole.valueOf(getClaims(token).get("role", String.class));
    }

    public MemberState extractState(String token) {
        return MemberState.valueOf(getClaims(token).get("state", String.class));
    }

    /** 토큰에서 Claims 추출 (검증 포함) */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpSeconds() {
        return props.accessTokenExpSeconds();
    }

    public long getRefreshTokenExpSeconds() {
        return props.refreshTokenExpSeconds();
    }
}
