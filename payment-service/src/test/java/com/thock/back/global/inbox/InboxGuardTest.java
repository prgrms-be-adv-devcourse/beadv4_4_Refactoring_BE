package com.thock.back.global.inbox;

import com.thock.back.global.inbox.repository.InboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxGuardTest {

    @Mock
    private InboxEventRepository inboxEventRepository;

    @InjectMocks
    private InboxGuard inboxGuard;

    @Test
    @DisplayName("claim 성공 시 true 반환")
    void tryClaim_success() {
        when(inboxEventRepository.claimIfAbsent("k1", "topic-a", "payment-service"))
                .thenReturn(1);

        boolean claimed = inboxGuard.tryClaim("k1", "topic-a", "payment-service");

        assertThat(claimed).isTrue();
    }

    @Test
    @DisplayName("unique 충돌 시 false 반환")
    void tryClaim_duplicate() {
        when(inboxEventRepository.claimIfAbsent("k1", "topic-a", "payment-service"))
                .thenReturn(0);

        boolean claimed = inboxGuard.tryClaim("k1", "topic-a", "payment-service");

        assertThat(claimed).isFalse();
    }
}
