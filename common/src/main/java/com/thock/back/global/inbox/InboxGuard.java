package com.thock.back.global.inbox;

import com.thock.back.global.inbox.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
        int inserted = inboxEventRepository.claimIfAbsent(idempotencyKey, topic, consumerGroup);

        // 이벤트 처음 처리
        if (inserted == 1) {
            return true;
        }

        // 이미 처리된 이벤트 (중복)
        log.info("Duplicate inbox message ignored: topic={}, consumerGroup={}, key={}", topic, consumerGroup, idempotencyKey);
        return false;
    }
}
