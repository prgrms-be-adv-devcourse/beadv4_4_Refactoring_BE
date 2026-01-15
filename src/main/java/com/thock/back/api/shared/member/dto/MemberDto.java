package com.thock.back.api.shared.member.dto;

import com.thock.back.api.shared.member.domain.Role;
import com.thock.back.api.shared.member.domain.State;
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
    private final Role role;
    private final State state;
}
