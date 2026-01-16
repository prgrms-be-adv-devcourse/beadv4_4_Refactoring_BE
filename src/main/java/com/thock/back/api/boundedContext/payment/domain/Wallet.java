package com.thock.back.api.boundedContext.payment.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import com.thock.back.api.shared.payment.dto.WalletDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_wallets")
public class Wallet extends BaseIdAndTime {
    @OneToOne(fetch = LAZY)
    private PaymentMember holder;

    private Long balance;

    private Long revenue;

    @OneToMany(mappedBy = "wallet", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<WalletLog> walletLogs = new ArrayList<>();;

    @OneToMany(mappedBy = "wallet", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<RevenueLog> revenueLogs = new ArrayList<>();;

    public Wallet(PaymentMember holder) {
        this.holder = holder;
        this.balance = 0L;
        this.revenue = 0L;
    }

    /**
     * 입금 관련 메서드
     **/

    public void depositBalance(Long amount, EventType eventType){
        this.balance += amount;
        addWalletLog(amount, eventType);
    }

    private void addWalletLog(Long amount, EventType eventType) {
        WalletLog walletLog = new WalletLog(
                this.holder,
                this,
                eventType,
                amount,
                this.balance
        );
        walletLogs.add(walletLog);
    }

    public void depositRevenue(Long amount, EventType eventType){
        this.revenue += amount;
        addRevenueLog(amount, eventType);
    }

    private void addRevenueLog(Long amount, EventType eventType) {
        RevenueLog revenueLog = new RevenueLog(
                this.holder,
                this,
                eventType,
                amount,
                this.revenue
        );
        revenueLogs.add(revenueLog);
    }

    /**
     * 출금 관련 메서드
     **/


    /**
     * WalletDto
     **/

        public WalletDto toDto(){
        return new WalletDto(
                getId(),
                getHolder().getId(),
                getHolder().getName(),
                getBalance(),
                getRevenue(),
                getCreatedAt(),
                getUpdatedAt()
        );
    }
}
