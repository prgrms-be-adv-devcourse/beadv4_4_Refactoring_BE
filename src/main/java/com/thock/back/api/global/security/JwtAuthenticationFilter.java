package com.thock.back.api.global.security;

import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveBearerToken(request);

        if (token != null && jwtTokenProvider.validate(token)) {
            Long memberId = jwtTokenProvider.extractMemberId(token);
            MemberRole role = jwtTokenProvider.extractRole(token);
            MemberState state = jwtTokenProvider.extractState(token);

            JwtPrincipal principal = new JwtPrincipal(memberId, role, state);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
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
