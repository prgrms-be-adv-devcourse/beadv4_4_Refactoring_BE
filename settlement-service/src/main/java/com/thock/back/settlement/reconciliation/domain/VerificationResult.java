package com.thock.back.settlement.reconciliation.domain;

import com.thock.back.global.jpa.entity.BaseCreatedTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "finance_reconciliation_verification_result", // 대사 결과 테이블
        indexes = {
                @Index(name = "idx_verify_base_date", columnList = "base_date"), // "어제 날짜 결과 줘" (필수)
                @Index(name = "idx_verify_status", columnList = "status"),       // "MISMATCH만 가져와" (필수)
                @Index(name = "idx_verify_order_no", columnList = "order_no")    // "이 주문 결과 어때?"
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationResult extends BaseCreatedTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_date", nullable = false) // 대사는 일자 단위기 때문에 LocalDate를 사용한다.
    private LocalDate baseDate;

    // 우리 내부 주문번호 (ORD-...)
    // PG 누락인 경우(PG_MISSING)에도 우리 주문번호는 있으므로 저장
    @Column(name = "order_no", nullable = false, length = 255)
    private String orderNo;

    // PG사 거래 번호 (tgen_...)
    // 우리 누락(INTERNAL_MISSING)인 경우를 대비해 nullable 가능성 열어둠
    @Column(name = "pg_key", length = 255)
    private String pgKey;

    // 대사 상태 (String, 길이 30 제한)
    // 값 예시: "MATCH", "MISMATCH", / "PG_MISSING", "INTERNAL_MISSING" 이건 일단 보류
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    // 차액 (PG금액 - 우리금액)
    // 0이면 정상, 양수/음수면 불일치. 정밀한 돈 계산을 위해 Decimal(18,4)
    @Column(name = "diff_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal diffAmount;

    // 실패 에러 로그 (불일치 사유)
    // 내용이 길어질 수 있으므로 TEXT 타입
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // createdAt. 결과 생성 시간 (대사 시작 시간) 상속받아 사용

    @Builder
    public VerificationResult(LocalDate baseDate, String orderNo, String pgKey, String status, BigDecimal diffAmount, String errorMessage){
        this.baseDate = baseDate;
        this.orderNo = orderNo;
        this.pgKey = pgKey;
        this.status = status;
        this.diffAmount = diffAmount != null ? diffAmount : BigDecimal.ZERO;
        this.errorMessage = errorMessage;
    }
}