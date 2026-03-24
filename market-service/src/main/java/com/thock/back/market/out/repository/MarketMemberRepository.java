package com.thock.back.market.out.repository;


import com.thock.back.market.domain.MarketMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MarketMemberRepository extends JpaRepository<MarketMember, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MarketMember m where m.id = :memberId")
    Optional<MarketMember> findByIdForUpdate(Long memberId);
}