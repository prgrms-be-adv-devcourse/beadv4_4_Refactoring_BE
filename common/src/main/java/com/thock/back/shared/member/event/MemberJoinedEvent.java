package com.thock.back.shared.member.event;

import com.thock.back.shared.member.dto.MemberDto;


public record MemberJoinedEvent (
        MemberDto member
)
{ }
