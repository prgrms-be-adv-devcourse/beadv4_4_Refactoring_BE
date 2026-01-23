package com.thock.back.api.boundedContext.settlement.in;

import com.thock.back.api.boundedContext.settlement.domain.SettlementMember;
import com.thock.back.api.boundedContext.settlement.out.SettlementMemberRepository;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final SettlementMemberRepository repository;

    @Override
    @Transactional
    public void run(String... args) {
        // 서버 켜질 때, 판매자 1명 강제로 가입시킴
        SettlementMember testSeller = SettlementMember.builder()
                .id(1L) // ID 1번
                .name("테스트판매자")
                .email("test@test.com")
                .bankCode("088")
                .role(MemberRole.SELLER)
                .state(MemberState.ACTIVE)
                .accountNumber("123-456-789")
                .accountHolder("김테스트")
                .build();

        repository.save(testSeller);
        System.out.println("========== [테스트] 가짜 판매자 등록 완료 ==========");
    }
}