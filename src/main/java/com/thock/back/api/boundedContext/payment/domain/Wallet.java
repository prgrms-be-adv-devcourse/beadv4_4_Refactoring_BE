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
    private PaymentMember member;

    private Long balance;

    private Long revenue;

    @OneToMany(mappedBy = "wallet", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<WalletLog> walletLogs = new ArrayList<>();;

    @OneToMany(mappedBy = "wallet", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<RevenueLog> revenueLogs = new ArrayList<>();;

    public Wallet(PaymentMember member) {
        this.member = member;
        this.balance = 0L;
        this.revenue = 0L;
    }

    public WalletDto toDto(){
        return new WalletDto(
                getId(),
                getMember().getId(),
                getMember().getName(),
                getBalance(),
                getRevenue(),
                getCreatedAt(),
                getUpdatedAt()
        );
    }
}
