package com.thock.back.api.boundedContext.settlement.domain;

import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.domain.ReplicaMember;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlement_members")
public class SettlementMember extends ReplicaMember {
    private String bankCode;
    private String accountNumber;
    private String accountHolder;

    @Builder
    public SettlementMember(Long id, String email, String name,
                            LocalDateTime originalCreatedAt, LocalDateTime originalUpdatedAt,
                            String bankName, String accountNumber, String accountHolder) {
        // 부모(ReplicaMember)에게 기본 정보 + 시간 정보 넘김
        super(email, name, MemberRole.SELLER, MemberState.ACTIVE,
                id, originalCreatedAt, originalUpdatedAt);

        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }
}
