package com.thock.back.shared.member.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRegisteredEvent {
    // 1. 식별자
    private Long memberId;

    // 2. 회원 기본 정보 (SettlementMember용)
    private String email;
    private String name;
    private LocalDateTime createdAt; // 원본 가입일
    private LocalDateTime updatedAt; // 원본 수정일

    // 3. 정산 계좌 정보 (SettlementMember용)
    private String bankCode;
    private String accountNumber;
    private String accountHolder;
}