package com.thock.back.member.infrastructure.history;

import com.thock.back.member.domain.entity.LoginHistory;
import com.thock.back.member.out.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 이력 기록을 담당하는 Infrastructure Service
 * 책임:
 * - 로그인 성공 이력 저장
 * - 로그인 실패 이력 저장 (향후 확장)
 * 위치 이유:
 * - 부수 효과(Side Effect)를 처리하는 인프라 로직
 * - 비동기 처리로 메인 비즈니스 로직과 분리
 * - 실패해도 메인 로직에 영향 없음
 **/

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginHistoryRecorder {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 로그인 성공 이력을 비동기로 기록
     * - @Async: 비동기 실행 (메인 트랜잭션과 분리)
     * - REQUIRES_NEW: 독립적인 트랜잭션 (실패해도 로그인 성공에 영향 없음)
     * @param memberId = 로그인한 회원 ID
     **/
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long memberId) {
        try {
            LoginHistory history = LoginHistory.success(memberId);
            loginHistoryRepository.save(history);

            log.debug("[HISTORY] Login success recorded. memberId={}", memberId);
        } catch (Exception e) {
            // 로그인 이력 저장 실패는 로그로 남기고 무시
            log.error("[HISTORY] Failed to record login history. memberId={}", memberId, e);
        }
    }

    /**
     * 로그인 실패 이력을 비동기로 기록 (향후 확장용)
     *
     * @param memberId 로그인 시도한 회원 ID (있는 경우)
     **/
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long memberId) {
        try {
            LoginHistory history = LoginHistory.fail(memberId);
            loginHistoryRepository.save(history);

            log.debug("[HISTORY] Login failure recorded. memberId={}", memberId);
        } catch (Exception e) {
            log.error("[HISTORY] Failed to record login failure. memberId={}", memberId, e);
        }
    }
}
