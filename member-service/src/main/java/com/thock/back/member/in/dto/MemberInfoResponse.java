package com.thock.back.member.in.dto;


import com.thock.back.shared.member.domain.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 정보 응답")
public record MemberInfoResponse (
        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "회원 권한", example = "SELLER")
        MemberRole role
) {}
