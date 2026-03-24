package com.thock.back.member.security;

import com.thock.back.global.security.JwtProperties;
import com.thock.back.global.security.JwtValidator;import com.thock.back.global.security.JwtValidatorImpl;import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Primary;import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
@Primary
public class JwtTokenProvider extends JwtValidatorImpl {

    private final JwtProperties props;

    public JwtTokenProvider(JwtProperties props) {
        super(props);
        this.props = props;
    }

    // AccessToken 생성
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

    // RefreshToken 생성
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

    public long getAccessTokenExpSeconds() {
        return props.accessTokenExpSeconds();
    }

    public long getRefreshTokenExpSeconds() {
        return props.refreshTokenExpSeconds();
    }
}
