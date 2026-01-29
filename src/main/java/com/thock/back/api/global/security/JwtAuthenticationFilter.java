package com.thock.back.api.global.security;

import com.thock.back.api.global.security.context.AuthMember;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveBearerToken(request);

        // ===== 1. 토큰 존재 여부 로그 =====
        if (token != null) {
            log.info("[JWT] Token exists. path={}, tokenPrefix={}",
                    request.getRequestURI(),
                    token.substring(0, Math.min(20, token.length())));
        } else {
            log.info("[JWT] Token is null. path={}", request.getRequestURI());
        }

        try {
            // ===== 2. 토큰 검증 =====
            if (token != null && jwtTokenProvider.validate(token)) {
                log.info("[JWT] Token is VALID. path={}", request.getRequestURI());

                Long memberId = jwtTokenProvider.extractMemberId(token);
                MemberRole role = jwtTokenProvider.extractRole(token);
                MemberState state = jwtTokenProvider.extractState(token);

                AuthMember principal = new AuthMember(memberId, role, state);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else if (token != null) {
                // ===== 3. 토큰이 있는데 검증 실패한 경우 =====
                log.warn("[JWT] Token is INVALID. path={}", request.getRequestURI());
                SecurityContextHolder.clearContext(); // 익명 사용자로 통과
            }
        } catch (Exception e) {
            // ===== 4. 파싱/검증 중 예외 =====
            log.error("[JWT] Token validation exception. path={}", request.getRequestURI(), e);
            SecurityContextHolder.clearContext(); // 403 만들지 말고 익명으로 처리
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader)) return null;

        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
