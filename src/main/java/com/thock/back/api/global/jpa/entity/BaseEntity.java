package com.thock.back.api.global.jpa.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {
    protected void publishEvent(Object event) {
        GlobalConfig.getEventPublisher().publish(event);
    }
    public abstract int getId();
    public abstract LocalDateTime getCreateDate();
    public abstract LocalDateTime getModifyDate();
}
