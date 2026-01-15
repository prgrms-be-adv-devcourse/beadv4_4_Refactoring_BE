package com.thock.back.api.boundedContext.payment.domain;

import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.domain.ReplicaMember;
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
    public PaymentMember(String email, String name, MemberState state, MemberRole role, Long id, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        super(email, name, role, state, id, createdAt, modifiedAt);
    }
}
