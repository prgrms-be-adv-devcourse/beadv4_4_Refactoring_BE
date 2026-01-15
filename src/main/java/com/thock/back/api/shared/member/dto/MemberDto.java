package com.thock.back.api.shared.member.dto;

import com.thock.back.api.boundedContext.member.domain.MemberRole;
import com.thock.back.api.boundedContext.member.domain.MemberState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class MemberDto {
    private final Long id;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String email;
    private final String name;
    private final MemberRole role;
    private final MemberState state;
}
