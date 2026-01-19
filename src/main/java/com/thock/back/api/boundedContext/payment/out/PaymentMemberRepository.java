package com.thock.back.api.boundedContext.payment.out;

import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMemberRepository extends JpaRepository<PaymentMember, Long> {
    Optional<PaymentMember> findByName(String name);
}
