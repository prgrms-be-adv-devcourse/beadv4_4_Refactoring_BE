package com.thock.back.shared.member.event;

import com.thock.back.shared.member.dto.MemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberModifiedEvent {
    private final MemberDto member;
}
