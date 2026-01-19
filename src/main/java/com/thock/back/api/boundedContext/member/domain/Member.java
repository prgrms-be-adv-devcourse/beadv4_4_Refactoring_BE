package com.thock.back.api.boundedContext.member.domain;

import com.thock.back.api.boundedContext.member.event.MemberSignedUpEvent;
import com.thock.back.api.shared.member.domain.MemberRole;
import com.thock.back.api.shared.member.domain.MemberState;
import com.thock.back.api.shared.member.domain.SourceMember;
import com.thock.back.api.shared.member.dto.MemberDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends SourceMember {

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    private LocalDateTime withdrawnAt;

    private Member(String email, String name) {
        super(email, name, MemberRole.USER, MemberState.ACTIVE);
    }

    public static Member signUp(String email, String name) {
        Member member = new Member(email, name);
        member.publishEvent(
                new MemberSignedUpEvent(
                new MemberDto(
                        member.getId(),
                        member.getCreatedAt(),
                        member.getUpdatedAt(),
                        member.getEmail(),
                        member.getName(),
                        member.getRole(),
                        member.getState()
                )));

        return member;
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.setState(MemberState.WITHDRAWN);
        this.withdrawnAt = LocalDateTime.now();
    }

    public MemberDto toDto(){
        return new MemberDto(
                getId(),
                getCreatedAt(),
                getUpdatedAt(),
                getEmail(),
                getName(),
                getRole(),
                getState()
        );
    }
}

