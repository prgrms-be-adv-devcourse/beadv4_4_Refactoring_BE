package com.thock.back.api.global.security;

import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import io.jsonwebtoken.Claims;
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
                .subject(String.valueOf(memberId)) // 표준 subject에 memberId
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(
                        "role", role,
                        "state", state
                ))
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

    /** 토큰에서 Claims 추출 (검증 포함) */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractMemberId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public String getRole(String token) {
        Claims claims = parseClaims(token);
        Object v = claims.get("role");
        return v == null ? null : v.toString();
    }

    public String getState(String token) {
        Claims claims = parseClaims(token);
        Object v = claims.get("state");
        return v == null ? null : v.toString();
    }

    public long getAccessTokenExpSeconds() {
        return props.accessTokenExpSeconds();
    }
}
