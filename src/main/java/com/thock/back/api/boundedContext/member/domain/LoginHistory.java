package com.thock.back.api.boundedContext.member.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_auth_login_history")
public class LoginHistory extends BaseIdAndTime {

    private Long memberId;
    private String email;
    private boolean success;
    private String failCode;
    private String ip;
    private String userAgent;
    private LocalDateTime attemptedAt;
}
