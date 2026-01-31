package com.thock.back.api.boundedContext.finance.reconciliation.domain;

import com.thock.back.api.global.jpa.entity.BaseCreatedTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pg_sales_raw",
        indexes = {
                @Index(name = "idx_pg_merchant_uid", columnList = "merchant_uid"),
                @Index(name = "idx_pg_transacted_at", columnList = "transacted_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// PG사 엑셀 데이터 내용
public class PgSalesRaw extends BaseCreatedTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "pg_key", length = 255) //varchar(255) / pg사의 고유 번호 (결제 번호 같은 느낌)
    private String pgKey;

    @Column(name = "merchant_uid", nullable = false, length = 100) //varchar(100), 주문 번호를 카드사에서 부르는 명칭
    private String merchantUid;

    @Column(name = "payment_method", length = 50) //varchar(50) or ENUM / POINT, CARD, ETC...
    private String paymentMethod;

    @Column(name = "payment_amount", nullable = false, precision = 18, scale = 4)// DECIMAL(18, 4)
    private BigDecimal paymentAmount;

    @Column(name = "pg_status", nullable = false, length = 50)
    //varchar(50) or ENUM  / PAID, CANCELED, FAILED
    private String pgStatus;

    @Column(name = "transacted_at", nullable = false)
    // 엑셀 데이터 속 있는 카드사가 받은 결제 승인 시간
    private LocalDateTime transactedAt;

    //createdAt 상속 받아 사용. 관리자가 엑셀 데이터를 입력한 시점

    @Builder
    public PgSalesRaw(String pgKey, String merchantUid, String paymentMethod, BigDecimal paymentAmount, String pgStatus, LocalDateTime transactedAt){
        this.pgKey = pgKey;
        this.merchantUid = merchantUid;
        this.paymentMethod = paymentMethod;
        this.paymentAmount = paymentAmount;
        this.pgStatus = pgStatus;
        this.transactedAt = transactedAt;
    }
}

