package com.thock.back.api.boundedContext.member.domain;

import com.thock.back.api.global.jpa.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_login_histories",
    indexes = @Index(name = "idx_login_history_member_id", columnList = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "logged_in_at", nullable = false)
    private LocalDateTime loggedInAt;

    private LoginHistory(Long memberId, boolean success, LocalDateTime loggedInAt) {
        this.memberId = memberId;
        this.loggedInAt = LocalDateTime.now();
        this.success = success;
    }

    public static LoginHistory success(Long memberId) {
        return new LoginHistory(memberId, true, LocalDateTime.now());
    }

    public static LoginHistory fail(Long memberId) {
        return new LoginHistory(memberId, false, LocalDateTime.now());
    }
}
