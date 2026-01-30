package com.thock.back.api.boundedContext.market.out.repository;

import com.thock.back.api.boundedContext.market.domain.MarketMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketMemberRepository extends JpaRepository<MarketMember, Long> {
}
