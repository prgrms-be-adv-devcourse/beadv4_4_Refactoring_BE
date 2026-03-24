package com.thock.back.payment.domain;


import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import com.thock.back.shared.member.domain.ReplicaMember;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_members")
public class PaymentMember extends ReplicaMember {
    private String bankCode;
    private String accountNumber;
    private String accountHolder;

    public PaymentMember(String email, String name, MemberState state, MemberRole role, Long id, LocalDateTime createdAt, LocalDateTime modifiedAt, String bankCode, String accountNumber, String accountHolder) {
        super(email, name, role, state, id, createdAt, modifiedAt);
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }
}

