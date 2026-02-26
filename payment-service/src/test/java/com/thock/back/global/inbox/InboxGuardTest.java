package com.thock.back.global.inbox;

import com.thock.back.global.inbox.repository.InboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
        when(inboxEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean claimed = inboxGuard.tryClaim("k1", "topic-a", "payment-service");

        assertThat(claimed).isTrue();
    }

    @Test
    @DisplayName("unique 충돌 시 false 반환")
    void tryClaim_duplicate() {
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(inboxEventRepository).saveAndFlush(any());

        boolean claimed = inboxGuard.tryClaim("k1", "topic-a", "payment-service");

        assertThat(claimed).isFalse();
    }
}
