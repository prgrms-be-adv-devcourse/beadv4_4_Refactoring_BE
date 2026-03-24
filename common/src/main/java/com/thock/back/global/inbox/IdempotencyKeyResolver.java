package com.thock.back.global.inbox;

/**
 * 이벤트/요청을 멱등성 키로 변환하는 전략 인터페이스.
 * 키 규칙은 도메인별로 달라 서비스 모듈에서 구현한다.
 */
public interface IdempotencyKeyResolver<T> {

    String resolve(T payload);
}
