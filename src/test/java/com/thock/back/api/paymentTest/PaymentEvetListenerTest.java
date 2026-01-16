package com.thock.back.api.paymentTest;

import com.thock.back.api.boundedContext.payment.app.PaymentFacade;
import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.dto.MemberDto;
import com.thock.back.api.shared.member.event.MemberJoinedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class PaymentEvetListenerTest {

    @Autowired
    EventPublisher eventPublisher;

    @Autowired
    PaymentMemberRepository paymentMemberRepository;

    @Autowired
    PlatformTransactionManager txManager;

    MemberDto memberDto = new MemberDto(
            1L,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now(),
            "test@example.com",
            "테스트유저",
            MemberRole.USER,
            MemberState.ACTIVE
    );

    @Test
    void PaymentSyncMember_테스트() {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        // 이벤트 발생 + 트랜잭션 commit
        tx.execute(status -> {
            eventPublisher.publish(new MemberJoinedEvent(memberDto));
            return null;
        });

        // DB 실제 검사
        var member = paymentMemberRepository.findById(memberDto.getId());

        assertThat(member).isPresent();
        assertThat(member.get().getEmail()).isEqualTo(memberDto.getEmail());
        assertThat(member.get().getName()).isEqualTo(memberDto.getName());
        assertThat(member.get().getRole()).isEqualTo(memberDto.getRole());
        assertThat(member.get().getState()).isEqualTo(memberDto.getState());
    }
}
