package com.thock.back.shared.market.dto;

import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;

import java.time.LocalDateTime;

public record MarketMemberDto (
        Long id,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String email,
        String name,
        MemberRole role,
        MemberState state,

        // 배송지 정보
        String zipCode,
        String baseAddress,
        String detailAddress

        // 계좌 정보
//    private final String bankCode;
//    private final String accountNumber;
//    private final String accountHolder;
)
{
}
