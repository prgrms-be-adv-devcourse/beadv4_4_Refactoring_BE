package com.thock.back.shared.market.dto;

import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MarketMemberDto {
    private final Long id;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String email;
    private final String name;
    private final MemberRole role;
    private final MemberState state;

    // 배송지 정보
    private final String zipCode;
    private final String baseAddress;
    private final String detailAddress;

    // 계좌 정보
//    private final String bankCode;
//    private final String accountNumber;
//    private final String accountHolder;
}
