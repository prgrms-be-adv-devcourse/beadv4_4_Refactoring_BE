package com.thock.back.market.out.repository;


import com.thock.back.market.domain.MarketMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketMemberRepository extends JpaRepository<MarketMember, Long> {
}
