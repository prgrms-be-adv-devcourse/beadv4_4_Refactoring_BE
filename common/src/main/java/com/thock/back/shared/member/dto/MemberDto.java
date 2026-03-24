package com.thock.back.shared.member.dto;

import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;

import java.time.LocalDateTime;

public record MemberDto (
        Long id,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String email,
        String name,
        MemberRole role,
        MemberState state,
        String bankCode,
        String accountNumber,
        String accountHolder
){

}
