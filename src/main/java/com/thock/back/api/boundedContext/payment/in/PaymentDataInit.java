package com.thock.back.api.boundedContext.payment.in;

import com.thock.back.api.boundedContext.payment.domain.Payment;
import com.thock.back.api.boundedContext.payment.domain.PaymentMember;
import com.thock.back.api.boundedContext.payment.domain.PaymentStatus;
import com.thock.back.api.boundedContext.payment.out.PaymentMemberRepository;
import com.thock.back.api.boundedContext.payment.out.PaymentRepository;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class PaymentDataInit {
    private final PaymentRepository paymentRepository;
    private final PaymentMemberRepository paymentMemberRepository;

    @Bean
    public ApplicationRunner baseInitDataRunner (){
        return args ->{
            addPayment();
        };
    }

    @Transactional
    public void addPayment(){
        PaymentMember member = new PaymentMember("test@example.com", "테스트유저", MemberState.ACTIVE, MemberRole.USER, 1L, null, null);
        paymentMemberRepository.save(member);
        Payment payment = new Payment(1_000L, member, PaymentStatus.REQUESTED, "Y-OwrraDVP2BQybbYbjs22y57123", 1_000L, "tgen_202601211141463Wo99");
        paymentRepository.save(payment);
    }
}
