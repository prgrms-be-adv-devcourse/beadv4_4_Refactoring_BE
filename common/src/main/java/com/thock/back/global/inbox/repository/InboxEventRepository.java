package com.thock.back.global.inbox.repository;

import com.thock.back.global.inbox.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {
}
