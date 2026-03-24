package com.thock.back.global.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedTime {

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
