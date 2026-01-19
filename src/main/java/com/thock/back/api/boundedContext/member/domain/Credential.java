package com.thock.back.api.boundedContext.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_credentials",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_credentials_member_id", columnNames = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Credential {

    @Id
    private Long memberId; // Member PK를 그대로 사용 (공유키)

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Credential(Long memberId, String passwordHash) {
        this.memberId = memberId;
        this.passwordHash = passwordHash;
        this.updatedAt = LocalDateTime.now();
    }

    public static Credential create(Long memberId, String passwordHash) {
        return new Credential(memberId, passwordHash);
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
        this.updatedAt = LocalDateTime.now();
    }
}
