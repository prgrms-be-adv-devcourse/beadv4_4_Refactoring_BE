package com.thock.back.shared.member.event;

import java.time.LocalDateTime;


public record SellerRegisteredEvent (
                // 1. 식별자
                Long memberId,

                // 2. 회원 기본 정보 (SettlementMember용)
                String email,
                String name,
                LocalDateTime createdAt, // 원본 가입일
                LocalDateTime updatedAt, // 원본 수정일

                // 3. 정산 계좌 정보 (SettlementMember용)
                String bankCode,
                String accountNumber,
                String accountHolder
) { }