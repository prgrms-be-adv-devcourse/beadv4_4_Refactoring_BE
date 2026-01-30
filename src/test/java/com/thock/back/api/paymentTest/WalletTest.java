//package com.thock.back.api.paymentTest;
//
//import com.thock.back.api.boundedContext.payment.domain.*;
//import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
//import com.thock.back.api.boundedContext.payment.out.WalletLogRepository;
//import com.thock.back.api.boundedContext.payment.out.WalletRepository;
//import com.thock.back.api.global.eventPublisher.EventPublisher;
//import com.thock.back.api.shared.member.domain.MemberRole;
//import com.thock.back.api.shared.member.domain.MemberState;
//import com.thock.back.api.shared.member.dto.MemberDto;
//import com.thock.back.api.shared.member.event.MemberJoinedEvent;
//import com.thock.back.api.shared.member.event.MemberModifiedEvent;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import java.time.LocalDateTime;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//
//@SpringBootTest
//public class WalletTest {
//    @Autowired
//    EventPublisher eventPublisher;
//
//    @Autowired
//    PaymentMemberRepository paymentMemberRepository;
//
//    @Autowired
//    WalletRepository walletRepository;
//
//    @Autowired
//    WalletLogRepository walletLogRepository;
//
//    @Autowired
//    PlatformTransactionManager txManager;
//
////    @Test
////    @DisplayName("지갑_금액_입금_및_지갑_로그_테스트")
////    void 지갑_입금_및_지갑_로그_테스트() {
////        // given
////        PaymentMember member = new PaymentMember(
////                "test@mail.com",
////                "tester",
////                MemberState.ACTIVE,
////                MemberRole.USER,
////                1L,
////                LocalDateTime.now(),
////                LocalDateTime.now()
////        );
////
////        Wallet wallet = new Wallet(member);
////
////        // when
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----지갑_입금_테스트 -----");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("----- 입금 전 -----");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("balance = " + wallet.getBalance().toString());
////        assertThat(wallet.getBalance()).isEqualTo(0L);
////        wallet.depositBalance(50_000L, EventType.입금);
////        System.out.println("-----------------------------------------------------");
////        System.out.println("----- 입금 후-----");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("balance = " + wallet.getBalance().toString());
////        // then
////        assertThat(wallet.getBalance()).isEqualTo(50_000L);
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----지갑_로그_테스트 -----");
////        System.out.println("-----------------------------------------------------");
////        WalletLog log = wallet.getWalletLogs().get(0);
////        assertThat(log.getAmount()).isEqualTo(50_000L);
////        assertThat(log.getEventType()).isEqualTo(EventType.입금);
////        assertThat(log.getBalance()).isEqualTo(50_000L);
////        System.out.println("log amount = " + log.getAmount());
////        System.out.println("log eventType = " + log.getEventType());
////        System.out.println("log balance = " + log.getBalance());
////    }
////
////    @Test
////    @DisplayName("지갑_판매수익_입금_및_판매수익_로그_테스트")
////    void 지갑_판매수익_입금_및_판매수익_로그_테스트() {
////        // given
////        PaymentMember member = new PaymentMember(
////                "test@mail.com",
////                "tester",
////                MemberState.ACTIVE,
////                MemberRole.USER,
////                1L,
////                LocalDateTime.now(),
////                LocalDateTime.now()
////        );
////
////        Wallet wallet = new Wallet(member);
////
////        // when
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----판매수익_입금_테스트 -----");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("----- 입금 전 -----");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("revenue = " + wallet.getRevenue().toString());
////        assertThat(wallet.getRevenue()).isEqualTo(0L);
////        wallet.depositRevenue(100_000L, EventType.입금);
////        System.out.println("-----------------------------------------------------");
////        System.out.println("----- 입금 후-----");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("revenue = " + wallet.getRevenue().toString());
////        // then
////        assertThat(wallet.getRevenue()).isEqualTo(100_000L);
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----판매수익_로그_테스트 -----");
////        System.out.println("-----------------------------------------------------");
////        RevenueLog log = wallet.getRevenueLogs().get(0);
////        assertThat(log.getAmount()).isEqualTo(100_000L);
////        assertThat(log.getEventType()).isEqualTo(EventType.입금);
////        assertThat(log.getBalance()).isEqualTo(100_000L);
////        System.out.println("log amount = " + log.getAmount());
////        System.out.println("log eventType = " + log.getEventType());
////        System.out.println("log revenue = " + log.getBalance());
////    }
////
////    @Test
////    @DisplayName("지갑_금액_출금_및_지갑_로그_테스트")
////    void 지갑_출금_및_지갑_로그_테스트() {
////        // given
////        PaymentMember member = new PaymentMember(
////                "test@mail.com",
////                "tester",
////                MemberState.ACTIVE,
////                MemberRole.USER,
////                1L,
////                LocalDateTime.now(),
////                LocalDateTime.now()
////        );
////
////        Wallet wallet = new Wallet(member);
////
////        // when
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----지갑_출금_테스트 -----");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("----- 출금 전 -----");
////        System.out.println("-----------------------------------------------------");
////        wallet.depositBalance(50_000L, EventType.입금);
////        System.out.println("balance = " + wallet.getBalance().toString());
////        assertThat(wallet.getBalance()).isEqualTo(50_000L);
////        System.out.println("-----------------------------------------------------");
////        System.out.println("----- 출금 후-----");
////        System.out.println("-----------------------------------------------------");
////        wallet.withdrawBalance(30_000L, EventType.출금);
////        System.out.println("balance = " + wallet.getBalance().toString());
////        // then
////        assertThat(wallet.getBalance()).isEqualTo(20_000L);
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----지갑_로그_테스트 -----");
////        System.out.println("-----------------------------------------------------");
////        WalletLog log = wallet.getWalletLogs().get(1);
////        assertThat(log.getAmount()).isEqualTo(30_000L);
////        assertThat(log.getEventType()).isEqualTo(EventType.출금);
////        assertThat(log.getBalance()).isEqualTo(20_000L);
////        System.out.println("log amount = " + log.getAmount());
////        System.out.println("log eventType = " + log.getEventType());
////        System.out.println("log balance = " + log.getBalance());
////    }
////
////    @Test
////    @DisplayName("지갑_판매수익_출금_및_판매수익_로그_테스트")
////    void 지갑_판매수익_출금_및_판매수익_로그_테스트() {
////        // given
////        PaymentMember member = new PaymentMember(
////                "test@mail.com",
////                "tester",
////                MemberState.ACTIVE,
////                MemberRole.USER,
////                1L,
////                LocalDateTime.now(),
////                LocalDateTime.now()
////        );
//
////        Wallet wallet = new Wallet(member);
////        // when
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----판매수익_출금_테스트 -----");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("----- 출금 전 -----");
////        System.out.println("-----------------------------------------------------");
////        System.out.println("revenue = " + wallet.getRevenue().toString());
////        assertThat(wallet.getRevenue()).isEqualTo(0L);
////        wallet.depositRevenue(100_000L, EventType.판매수익_입금);
////        System.out.println("-----------------------------------------------------");
////        System.out.println("----- 출금 후-----");
////        System.out.println("-----------------------------------------------------");
////        wallet.withdrawRevenue(30_000L, EventType.판매수익_출금);
////        System.out.println("revenue = " + wallet.getRevenue().toString());
////        // then
////        assertThat(wallet.getRevenue()).isEqualTo(70_000L);
////        System.out.println("-----------------------------------------------------");
////        System.out.println("-----판매수익_로그_테스트 -----");
////        System.out.println("-----------------------------------------------------");
////        RevenueLog log = wallet.getRevenueLogs().get(1);
////        assertThat(log.getAmount()).isEqualTo(30_000L);
////        assertThat(log.getEventType()).isEqualTo(EventType.판매수익_출금);
////        assertThat(log.getBalance()).isEqualTo(70_000L);
////        System.out.println("log amount = " + log.getAmount());
////        System.out.println("log eventType = " + log.getEventType());
////        System.out.println("log revenue = " + log.getBalance());
////    }
//}
