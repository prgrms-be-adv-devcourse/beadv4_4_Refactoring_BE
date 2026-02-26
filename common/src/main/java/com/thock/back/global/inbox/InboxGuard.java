package com.thock.back.global.inbox;

import com.thock.back.global.inbox.entity.InboxEvent;
import com.thock.back.global.inbox.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "inbox", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class InboxGuard {

    private final InboxEventRepository inboxEventRepository;

    @Transactional
    public boolean tryClaim(String idempotencyKey, String topic, String consumerGroup) {
        try {
            // 중복 키 유입 여부를 비즈니스 로직 전에 즉시 판별한다.
            inboxEventRepository.saveAndFlush(
                    InboxEvent.create(idempotencyKey, topic, consumerGroup)
            );
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate inbox message ignored: topic={}, consumerGroup={}, key={}",
                    topic, consumerGroup, idempotencyKey);
            return false;
        }
    }
}
