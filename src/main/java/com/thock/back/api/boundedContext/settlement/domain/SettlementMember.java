package com.thock.back.api.boundedContext.settlement.domain;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.domain.ReplicaMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "settlement_members") // 테이블 이름 변경
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementMember extends ReplicaMember {

    @Id
    @Column(name = "id") // Member 모듈의 ID를 그대로 사용 (Auto Increment 아님)
    private Long id;

    private String email;
    private String name;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private MemberState state;

    // 판매자일 때만 값이 들어옴 (nullable)
    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    @Builder
    public SettlementMember(Long id, String email, String name, MemberRole role, MemberState state,
                            String bankCode, String accountNumber, String accountHolder) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.state = state;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }

    // [동기화용] MemberDto 정보로 업데이트하는 메서드
    public void syncFromMember(String email, String name, MemberRole role, MemberState state,
                               String bankCode, String accountNumber, String accountHolder) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.state = state;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }
}