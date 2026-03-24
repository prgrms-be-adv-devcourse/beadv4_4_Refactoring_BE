package com.thock.back.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_INTERNAL_AUTH = "X-Internal-Auth";

    // 새 키가 있으면 새 키 사용, 없으면 기존 gateway 키 fallback
    @Value("${SECURITY_SERVICE_INTERNAL_SECRET:${SECURITY_GATEWAY_INTERNAL_SECRET:}}")
    private String internalSecret;

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 내부 API만 검사
        return uri == null || !uri.contains("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (internalSecret == null || internalSecret.isBlank()) {
            log.error("SECURITY_SERVICE_INTERNAL_SECRET is missing.");
            writeError(response, ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
            return;
        }
        String headerValue = request.getHeader(HEADER_INTERNAL_AUTH);
        if (headerValue == null || !internalSecret.equals(headerValue)) {
            log.warn("Invalid internal auth header. path={}", request.getRequestURI());
            writeError(response, ErrorCode.AUTH_CONTEXT_NOT_FOUND, request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String path) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = ErrorResponse.of(errorCode, path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}