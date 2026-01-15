package com.thock.back.api.shared.member.domain;


import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class ReplicaMember extends BaseMember {
    @Id
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReplicaMember(String email, String name, Role role, State state,long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(email, name, role, state); //TODO: 변경
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}