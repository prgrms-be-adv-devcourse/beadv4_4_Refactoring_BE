package com.thock.back.api.shared.member.domain;

import com.thock.back.api.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class BaseMember extends BaseEntity {
    // 모든 상속받는 멤버가 다 가지고있어야하는 필드
    @Column(unique = true)
    private String email;

    private String name;

    private Role role;

    private State state;

    public BaseMember(String email, String name, Role role, State state) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.state = state;
    }
}
