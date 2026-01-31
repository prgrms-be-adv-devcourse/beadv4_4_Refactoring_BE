package com.thock.back.global.jpa.entity;

import com.thock.back.global.config.GlobalConfig;
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
    public abstract Long getId();
    public abstract LocalDateTime getCreatedAt();
    public abstract LocalDateTime getUpdatedAt();
}
