package com.thock.back.market.monitoring.sql;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class MarketRequestSqlMetricsFilter extends OncePerRequestFilter {

    private final MarketRequestSqlMetricsRecorder metricsRecorder;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MarketRequestSqlMetricsContext.start(request.getMethod(), request.getRequestURI());
        try {
            filterChain.doFilter(request, response);
        } finally {
            Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String uri = bestMatchingPattern != null ? bestMatchingPattern.toString() : request.getRequestURI();

            MarketRequestSqlMetricsContext.Snapshot snapshot = MarketRequestSqlMetricsContext.finish();
            if (snapshot != null) {
                metricsRecorder.record(
                        new MarketRequestSqlMetricsContext.Snapshot(
                                snapshot.method(),
                                uri,
                                snapshot.totalQueries(),
                                snapshot.selectQueries(),
                                snapshot.insertQueries(),
                                snapshot.updateQueries(),
                                snapshot.deleteQueries(),
                                snapshot.totalQueryTimeMs()
                        ),
                        response.getStatus()
                );
            }
        }
    }
}
