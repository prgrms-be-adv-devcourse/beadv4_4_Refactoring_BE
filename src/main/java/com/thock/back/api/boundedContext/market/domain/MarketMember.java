package com.thock.back.api.boundedContext.market.domain;

import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.domain.ReplicaMember;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_members")
@NoArgsConstructor
@Getter
public class MarketMember extends ReplicaMember {
    public MarketMember(String email,
                        String name,
                        MemberRole role,
                        MemberState state,
                        Long id,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt){
        super(email, name, role, state, id, createdAt, updatedAt);
    }
}
