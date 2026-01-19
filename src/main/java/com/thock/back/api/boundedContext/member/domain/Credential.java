package com.thock.back.api.boundedContext.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_credentials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "failed_count", nullable = false)
    private int failedCount = 0;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "password_updated_at", nullable = false)
    private LocalDateTime passwordUpdatedAt;

    private Credential(Member member, String passwordHash) {
        this.member = member;
        this.passwordHash = passwordHash;
        this.passwordUpdatedAt = LocalDateTime.now();
    }

    public static Credential create(Member member, String passwordHash) {
        return new Credential(member, passwordHash);
    }
}
