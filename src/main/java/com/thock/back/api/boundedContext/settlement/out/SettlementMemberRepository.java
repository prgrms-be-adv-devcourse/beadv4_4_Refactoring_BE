package com.thock.back.api.boundedContext.settlement.out;

import com.thock.back.api.boundedContext.settlement.domain.SettlementMember;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SettlementMemberRepository extends JpaRepository <SettlementMember, Long> {
    boolean existsById(Long id);
}
