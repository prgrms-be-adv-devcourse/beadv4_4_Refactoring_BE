package com.thock.back.gateway.filter;

import com.thock.back.global.security.JwtValidator;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class GatewayJwtGatewayFilterFactory extends AbstractGatewayFilterFactory<GatewayJwtGatewayFilterFactory.Config> {

    private final JwtValidator jwtValidator;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public GatewayJwtGatewayFilterFactory(JwtValidator jwtValidator) {
        super(Config.class);
        this.jwtValidator = jwtValidator;
    }

    public static class Config {
        private List<String> excludePaths;
        private List<String> excludeGetPaths;

        public List<String> getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(List<String> excludePaths) {
            this.excludePaths = excludePaths;
        }

        public List<String> getExcludeGetPaths() {
            return excludeGetPaths;
        }

        public void setExcludeGetPaths(List<String> excludeGetPaths) {
            this.excludeGetPaths = excludeGetPaths;
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 1. 인증 제외 경로 체크
            if (config.getExcludePaths() != null) {
                for (String excludePath : config.getExcludePaths()) {
                    if (pathMatcher.match(excludePath, path)) {
                        log.info("[Gateway] Auth skipped for path: {}", path);
                        return chain.filter(exchange);
                    }
                }
            }

            // 1-1. GET 전용 인증 제외 경로 체크 (읽기 API만 공개)
            if (request.getMethod() == HttpMethod.GET && config.getExcludeGetPaths() != null) {
                for (String excludePath : config.getExcludeGetPaths()) {
                    if (pathMatcher.match(excludePath, path)) {
                        // "/api/v1/products/me" is a protected GET endpoint and must carry auth headers.
                         if (pathMatcher.match("/api/v1/products/me", path)) {
                            break;
                          }

                        log.info("[Gateway] Auth skipped for GET path: {}", path);
                        return chain.filter(exchange);
                    }
                }
            }

            // 2. Authorization 헤더 추출
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[Gateway] Missing or invalid Authorization header for path: {}", path);
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // 3. JWT 검증
            if (!jwtValidator.validate(token)) {
                log.error("[Gateway] JWT validation failed for path: {}", path);
                return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }

            // 4. 정보 추출
            try {
                Long memberId = jwtValidator.extractMemberId(token);
                MemberRole role = jwtValidator.extractRole(token);
                MemberState state = jwtValidator.extractState(token);

                // 5. 검증된 정보를 헤더에 추가
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-Member-Id", String.valueOf(memberId))
                        .header("X-Member-Role", role.name())
                        .header("X-Member-State", state.name())
                        .build();

                log.info("[Gateway] JWT authenticated - memberId: {}, role: {}, state: {}, path: {}",
                        memberId, role, state, path);

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.error("[Gateway] Failed to extract JWT claims: {}", e.getMessage());
                return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange,
                               String message,
                               HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String errorJson = String.format("{\"error\": \"%s\", \"message\": \"%s\"}",
                status.name(), message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorJson.getBytes())));
    }
}
