package com.thock.back.api.boundedContext.member.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "member_auth_refresh_token")
public class RefreshToken extends BaseIdAndTime {

    @ManyToOne(fetch = LAZY)
    private Member member;

    private String tokenHash;
    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime revokedAt;


}
