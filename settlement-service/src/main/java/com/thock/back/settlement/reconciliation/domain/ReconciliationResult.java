package com.thock.back.settlement.reconciliation.domain;

import com.github.f4b6a3.tsid.TsidCreator;
import com.thock.back.settlement.reconciliation.domain.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reconciliation_job")
public class ReconciliationResult {

    @Id
    private Long id; // TSID

    @Column(nullable = false)
    private LocalDate baseDate; // 대사 기준일 (예: 2026-02-06)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status; // RUNNING, SUCCESS, FAIL, WARNING

    private Integer totalCount;    // 총 비교 건수
    private Integer matchCount;    // 일치 건수
    private Integer mismatchCount; // 불일치 건수

    private LocalDateTime startedAt;  // 배치 시작 시간
    private LocalDateTime finishedAt; // 배치 종료 시간

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // 시스템 에러 등으로 배치가 터졌을 때 사유

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = TsidCreator.getTsid().toLong();
        }
        if (this.status == null) {
            this.status = JobStatus.RUNNING;
        }
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }

    public ReconciliationResult(LocalDate baseDate) {
        this.baseDate = baseDate;
        this.status = JobStatus.RUNNING;       // 생성 시점엔 무조건 RUNNING
        this.startedAt = LocalDateTime.now();  // 생성 시점이 시작 시간
        this.totalCount = 0;
        this.matchCount = 0;
        this.mismatchCount = 0;
    }

    // 배치 종료 시 업데이트를 위한 편의 메서드
    public void finish(int total, int match, int mismatch) {
        this.totalCount = total;
        this.matchCount = match;
        this.mismatchCount = mismatch;
        this.finishedAt = LocalDateTime.now();

        if (mismatch > 0) {
            this.status = JobStatus.WARNING; // 불일치가 있으면 경고
        } else {
            this.status = JobStatus.SUCCESS; // 완벽하면 성공
        }
    }

    public void fail(String errorMessage) {
        this.status = JobStatus.FAIL;
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }
}