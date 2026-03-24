package com.thock.back.member.domain.entity;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import jakarta.persistence.*;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import com.thock.back.shared.member.domain.SourceMember;
import com.thock.back.shared.member.dto.MemberDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Entity
@Table(name = "member_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends SourceMember {

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    private Member(String email, String name) {
        super(email, name, MemberRole.USER, MemberState.ACTIVE);
    }

    public static Member signUp(String email, String name) {
        return new Member(email, name);
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.setState(MemberState.WITHDRAWN);
        this.withdrawnAt = LocalDateTime.now();
    }

    public boolean isWithdrawn() {
        return this.getState() == MemberState.WITHDRAWN;
    }

    public boolean isInActive() {
        return this.getState() == MemberState.INACTIVE;
    }

    public void promoteToSeller(String bankCode, String accountNumber, String accountHolder) {
        if (this.getRole() != MemberRole.USER) {
            throw new CustomException(ErrorCode.INVALID_ROLE_PROMOTION);
        }

        if (bankCode == null || accountNumber == null || accountHolder == null) {
            throw new CustomException(ErrorCode.MISSING_BANKING_INFORMATION);
        }

        log.info("[MEMBER] Member promoted to seller. memberId={}", this.getId());

        this.setRole(MemberRole.SELLER);
        this.setBankCode(bankCode);
        this.setAccountNumber(accountNumber);
        this.setAccountHolder(accountHolder);
    }

    public MemberDto toDto(){
        return new MemberDto(
                getId(),
                getCreatedAt(),
                getUpdatedAt(),
                getEmail(),
                getName(),
                getRole(),
                getState(),
                getBankCode(),
                getAccountNumber(),
                getAccountHolder()
        );
    }
}

