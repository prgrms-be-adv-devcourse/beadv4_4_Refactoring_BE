package com.thock.back.api;

import com.thock.back.api.boundedContext.Member;
import com.thock.back.api.shared.member.dto.MemberDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@SpringBootTest
public class MemberTest {

    @Test
    public void testMemberModifiedEvent() {
        Member member = new Member(
                "asd", "asd","222","222"
        );
        MemberDto memberDto = new MemberDto(member.getId(), member.getCreateDate(), member.getModifyDate(), member.getEmail(), member.getName());
        System.out.println(memberDto.getName() + " " + memberDto.getCreateDate() + " " + memberDto.getModifyDate() + " " + memberDto.getEmail() + " " + memberDto.getId());
        System.out.println(member.getId() + " " + member.getCreateDate() + " " + member.getModifyDate() + " " + member.getEmail() + " " + member.getName() + " " + member.getPhone_number() + " " + member.getState_reason());
    }
}
