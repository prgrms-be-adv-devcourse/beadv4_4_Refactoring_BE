package com.thock.back.api.boundedContext.member.out;

import com.thock.back.api.boundedContext.member.domain.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
}
