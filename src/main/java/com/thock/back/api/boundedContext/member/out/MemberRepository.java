package com.thock.back.api.boundedContext.member.out;

import com.thock.back.api.boundedContext.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
}
