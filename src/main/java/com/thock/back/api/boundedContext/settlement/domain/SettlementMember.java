package com.thock.back.api.boundedContext.settlement.domain;

import com.thock.back.api.shared.member.domain.ReplicaMember;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "settlement_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementMember extends ReplicaMember { // implements Persistable 제거!

    // 필드는 부모(ReplicaMember) 거 씀 (선언 X)

    @Builder
    public SettlementMember(Long id, String email, String name, MemberRole role, MemberState state,
                            String bankCode, String accountNumber, String accountHolder,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {

        // 1. 부모 생성자에 원본(Member)의 시간 정보를 그대로 전달
        super(email, name, role, state, id, createdAt, updatedAt);

        // 2. 은행 정보 세팅
        this.setBankCode(bankCode);
        this.setAccountNumber(accountNumber);
        this.setAccountHolder(accountHolder);
    }

    // [동기화 메서드] - 이름 update로 변경하신 거 아주 좋습니다 (직관적!)
    public void update(String email, String name, MemberRole role, MemberState state,
                       String bankCode, String accountNumber, String accountHolder,
                       LocalDateTime updatedAt) {

        // 부모 필드 업데이트 (ReplicaMember/BaseMember의 Setter 사용)
        this.setEmail(email);
        this.setName(name);
        this.setRole(role);
        this.setState(state);

        // 은행 정보 업데이트
        this.setBankCode(bankCode);
        this.setAccountNumber(accountNumber);
        this.setAccountHolder(accountHolder);

        // 수정일 동기화
        this.setUpdatedAt(updatedAt);
    }
}