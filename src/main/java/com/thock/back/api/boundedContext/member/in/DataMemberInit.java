//package com.thock.back.api.boundedContext.member.in;
//
//import com.thock.back.api.boundedContext.member.domain.entity.Member;
//import com.thock.back.api.boundedContext.member.out.MemberRepository;
//import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
//import com.thock.back.api.boundedContext.payment.domain.Wallet;
//import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository; // Repository 필요!
//import com.thock.back.api.boundedContext.payment.out.WalletRepository;       // Repository 필요!
//import com.thock.back.api.boundedContext.settlement.domain.SettlementMember;
//import com.thock.back.api.boundedContext.settlement.out.SettlementMemberRepository;
//import com.thock.back.api.shared.member.domain.MemberRole;
//import com.thock.back.api.shared.member.domain.MemberState;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//@Component
//@RequiredArgsConstructor
//public class DataMemberInit implements CommandLineRunner {
//
//    private final MemberRepository memberRepository;
//    private final PaymentMemberRepository paymentMemberRepository;
//    private final WalletRepository walletRepository;
//    private final SettlementMemberRepository settlementMemberRepository; // 👈 여기 추가!
//
//    @Override
//    @Transactional
//    public void run(String... args) {
//        String email = "test@test.com";
//        if (memberRepository.existsByEmail(email)) return;
//
//        // 1. [Member] 생성 (ID: 1 자동 생성)
//        Member member = Member.signUp(email, "테스트판매자");
//        member.setRole(MemberRole.SELLER);
//        member.setState(MemberState.ACTIVE);
//        member.setBankCode("088");
//        member.setAccountNumber("123-456-789");
//        member.setAccountHolder("김테스트");
//        memberRepository.save(member);
//
//        // 2. [Settlement] 정산 멤버 생성 (ID: 1 동기화) 👈 이게 핵심!
//        SettlementMember settlementMember = SettlementMember.builder()
//                .id(member.getId()) // 1번 ID 그대로 사용
//                .email(member.getEmail())
//                .name(member.getName())
//                .role(member.getRole())
//                .state(member.getState())
//                .bankCode(member.getBankCode())
//                .accountNumber(member.getAccountNumber())
//                .accountHolder(member.getAccountHolder())
//                .createdAt(member.getCreatedAt())
//                .updatedAt(member.getUpdatedAt())
//                .build();
//        settlementMemberRepository.save(settlementMember);
//
//        // 3. [Payment] 결제 멤버 생성 (ID: 1 동기화)
//        PaymentMember paymentMember = new PaymentMember(
//                member.getEmail(), member.getName(), member.getState(), member.getRole(),
//                member.getId(), member.getCreatedAt(), member.getUpdatedAt()
//        );
//        paymentMemberRepository.save(paymentMember);
//
//        // 4. [Wallet] 지갑 생성
//        Wallet wallet = new Wallet(paymentMember);
//        walletRepository.save(wallet);
//
//        System.out.println("========== [테스트] ID 1번으로 Member/Settlement/Payment/Wallet 통합 생성 완료 ==========");
//    }
//}