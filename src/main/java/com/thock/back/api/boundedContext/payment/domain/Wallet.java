package com.thock.back.api.boundedContext.payment.domain;

import com.thock.back.api.global.exception.CustomException;
import com.thock.back.api.global.exception.ErrorCode;
import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.payment.dto.WalletDto;
import com.thock.back.api.shared.payment.event.PaymentAddBalanceLogEvent;
import com.thock.back.api.shared.payment.event.PaymentAddPaymentLogEvent;
import com.thock.back.api.shared.payment.event.PaymentAddRevenueLogEvent;
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
    //TODO: 멤버 유효성 검증 해야함
    @OneToOne(fetch = LAZY)
    private PaymentMember holder;

    private Long balance;

    private Long revenue;

//    @OneToMany(mappedBy = "wallet")
//    private List<WalletLog> walletLogs = new ArrayList<>();;
//
//    @OneToMany(mappedBy = "wallet")
//    private List<RevenueLog> revenueLogs = new ArrayList<>();;

    public Wallet(PaymentMember holder) {
        this.holder = holder;
        this.balance = 0L;
        this.revenue = 0L;
    }

    /**
     * 입금 관련 메서드
     **/

    public void depositBalance(Long amount){
        if(!isHolderStateOK(this.holder.getState())){
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }
        this.balance += amount;
    }



    public void depositRevenue(Long amount){
        if(!isHolderStateOK(this.holder.getState())){
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }
        this.revenue += amount;

    }

    /**
     * 출금 관련 메서드
     **/

    public void withdrawBalance(Long amount){
        if (balance < amount){
            throw new CustomException(ErrorCode.WALLET_NOT_WITHDRAW);
        }


        else if(!isHolderStateOK(this.holder.getState())){
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }

        this.balance -= amount;
    }

    public void withdrawRevenue(Long amount){
        if (revenue < amount){
            throw new CustomException(ErrorCode.WALLET_NOT_WITHDRAW);
        }

        else if(revenue == 0) {
            throw new CustomException(ErrorCode.WALLET_NOT_WITHDRAW);
        }

        else if(!isHolderStateOK(this.holder.getState())){
            throw new CustomException(ErrorCode.WALLET_IS_LOCKED);
        }

        this.revenue -= amount;
    }

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

    public boolean isHolderStateOK(MemberState memberState) {
        if(memberState == MemberState.SUSPENDED || memberState == MemberState.WITHDRAWN){
            return false;
        }
        return true;
    }

    /**
     * 로그 이벤트 관련
     */

    public void createBalanceLogEvent(Long amount, EventType eventType){
        publishEvent(
                new PaymentAddBalanceLogEvent(
                        toDto(),
                        eventType,
                        amount
                )
        );
    }
    public void createRevenueLogEvent(Long amount, EventType eventType){
        publishEvent(
                new PaymentAddRevenueLogEvent(
                        toDto(),
                        eventType,
                        amount
                )
        );
    }
}
