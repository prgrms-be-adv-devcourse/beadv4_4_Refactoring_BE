package com.thock.back.api.boundedContext.payment.domain;

import com.thock.back.api.shared.member.domain.ReplicaMember;
import com.thock.back.api.shared.member.domain.Role;
import com.thock.back.api.shared.member.domain.State;
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
    public PaymentMember(String email, String name, State state, Role role, Long id, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        super(email, name, role, state, id, createdAt, modifiedAt);
    }
}
