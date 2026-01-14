package com.thock.back.api.shared.member.event;

import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberJoinedEvent {
    private final MemberDto member;
}
