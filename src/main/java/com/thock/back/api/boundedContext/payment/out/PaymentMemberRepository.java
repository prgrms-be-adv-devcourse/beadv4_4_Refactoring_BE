package com.thock.back.api.boundedContext.payment.out;

import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMemberRepository extends JpaRepository<PaymentMember, Long> {
}
