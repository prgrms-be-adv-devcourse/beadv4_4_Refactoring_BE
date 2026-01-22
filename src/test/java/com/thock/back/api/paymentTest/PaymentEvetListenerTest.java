package com.thock.back.api.paymentTest;

import com.thock.back.api.boundedContext.payment.app.PaymentFacade;
import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
import com.thock.back.api.boundedContext.payment.out.WalletRepository;
import com.thock.back.api.global.eventPublisher.EventPublisher;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.dto.MemberDto;
import com.thock.back.api.shared.member.event.MemberJoinedEvent;
import com.thock.back.api.shared.member.event.MemberModifiedEvent;
import org.junit.jupiter.api.DisplayName;
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
    WalletRepository walletRepository;

    @Autowired
    PlatformTransactionManager txManager;



//    @Test
//    @DisplayName("MemberJoinedEvent_PaymentSyncMember_테스트")
//    void MemberJoinedEvent_PaymentSyncMember_테스트() {
//        TransactionTemplate tx = new TransactionTemplate(txManager);
//
//        MemberDto memberDto = new MemberDto(
//                1L,
//                LocalDateTime.now().minusDays(1),
//                LocalDateTime.now(),
//                "test@example.com",
//                "테스트유저",
//                MemberRole.USER,
//                MemberState.ACTIVE
//        );
//
//        // 이벤트 발생 + 트랜잭션 commit
//        tx.execute(status -> {
//            eventPublisher.publish(new MemberJoinedEvent(memberDto));
//            return null;
//        });
//
//        // DB 실제 검사
//        var member = paymentMemberRepository.findById(memberDto.getId());
//
//        System.out.println("-----------------------------------------------------");
//        System.out.println("-----MemberJoinedEvent_PaymentSyncMember_테스트 + 지갑 생성-----");
//        System.out.println("-----------------------------------------------------");
//        assertThat(member).isPresent();
//        assertThat(member.get().getEmail()).isEqualTo(memberDto.getEmail());
//        assertThat(member.get().getName()).isEqualTo(memberDto.getName());
//        assertThat(member.get().getRole()).isEqualTo(memberDto.getRole());
//        assertThat(member.get().getState()).isEqualTo(memberDto.getState());
//
//        // 지갑 생성 테스트
//        var wallet = walletRepository.findById(member.get().getId());
//        assertThat(wallet).isPresent();
//        assertThat(wallet.get().getHolder().getId()).isEqualTo(member.get().getId());
//        System.out.println("wallet member id = " + wallet.get().getHolder().getId());
//        System.out.println("wallet wallet id = " + wallet.get().getId());
//        System.out.println("wallet balance = " + wallet.get().getBalance());
//        System.out.println("wallet revenue = " + wallet.get().getRevenue());
//        System.out.println("paymentMember id = " + member.get().getId());
//        System.out.println("-----------------------------------------------------");
//        System.out.println("-----------------------------------------------------");
//        System.out.println("-----------------------------------------------------");
//    }
//
//    @Test
//    @DisplayName("MemberModifiedEvent_PaymentSyncMember_테스트")
//    void MemberModifiedEvent_PaymentSyncMember_테스트() {
//        TransactionTemplate tx = new TransactionTemplate(txManager);
//
//        MemberDto memberDto = new MemberDto(
//                1L,
//                LocalDateTime.now().minusDays(1),
//                LocalDateTime.now(),
//                "test@example.com",
//                "테스트유저",
//                MemberRole.USER,
//                MemberState.SUSPENDED
//        );
//
//
//        // 이벤트 발생 + 트랜잭션 commit
//        tx.execute(status -> {
//            eventPublisher.publish(new MemberModifiedEvent(memberDto));
//            return null;
//        });
//
//        // DB 실제 검사
//        var member = paymentMemberRepository.findById(memberDto.getId());
//
//        assertThat(member).isPresent();
//        assertThat(member.get().getEmail()).isEqualTo(memberDto.getEmail());
//        assertThat(member.get().getName()).isEqualTo(memberDto.getName());
//        assertThat(member.get().getRole()).isEqualTo(memberDto.getRole());
//        assertThat(member.get().getState()).isEqualTo(memberDto.getState());
//        System.out.println("-----------------------------------------------------");
//        System.out.println("-----MemberModifiedEvent_PaymentSyncMember_테스트 + 지갑 생성 -----");
//        System.out.println("-----------------------------------------------------");
//        System.out.println("member state = " + member.get().getState());
//
//
//        // 지갑 생성 테스트
//        var wallet = walletRepository.findById(member.get().getId());
//        assertThat(wallet).isPresent();
//        assertThat(wallet.get().getHolder().getId()).isEqualTo(member.get().getId());
//        System.out.println("wallet member id = " + wallet.get().getHolder().getId());
//        System.out.println("wallet wallet id = " + wallet.get().getId());
//        System.out.println("wallet balance = " + wallet.get().getBalance());
//        System.out.println("wallet revenue = " + wallet.get().getRevenue());
//        System.out.println("paymentMember id = " + member.get().getId());
//        System.out.println("-----------------------------------------------------");
//        System.out.println("-----------------------------------------------------");
//        System.out.println("-----------------------------------------------------");
//    }
}
