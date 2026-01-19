package com.thock.back.api.boundedContext.member.event;

import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberSignedUpEvent {
    private final MemberDto member;
}
