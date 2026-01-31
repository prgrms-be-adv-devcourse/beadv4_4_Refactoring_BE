package com.thock.back.api.boundedContext.finance.settlement.domain;

import com.thock.back.api.boundedContext.finance.global.jpa.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "finance_settlement_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // 은행 코드
    @Column(name = "bank_code", nullable = false, length = 50)
    private String bankCode;

    // 계좌번호
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    // 예금주
    @Column(name = "account_holder", nullable = false, length = 50)
    private String accountHolder;

    //createdAt, updatedAt 상속받아 사용

    @Builder
    public SettlementAccount(Long sellerId, String bankCode, String accountNumber, String accountHolder) {
        this.sellerId = sellerId;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }

}