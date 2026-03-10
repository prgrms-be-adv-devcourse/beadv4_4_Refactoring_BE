# DB 스키마 변경 운영 가이드 (ddl-auto=validate 기준)

`spring.jpa.hibernate.ddl-auto=validate` 운영 정책에서 안전하게 DB 스키마를 변경/배포하는 절차를 정의합니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.2 |
| 최종 수정일 | 2026-03-09 |
| 수정자 | ops-admin |

---

## 1) 핵심 원칙
- 운영 환경에서는 애플리케이션이 스키마를 자동 생성/수정하지 않습니다.
- 스키마 변경은 반드시 명시적 SQL(또는 마이그레이션 도구)로 수행합니다.
- "스키마 선반영 -> 애플리케이션 배포" 순서를 기본으로 합니다.

---

## 2) 변경 유형 분류
### 2-1. 하위 호환(Backward Compatible) 변경
- 컬럼 추가(기본값/nullable 고려)
- 인덱스 추가
- 새 테이블 추가

권장:
- 먼저 DB 반영 후 애플리케이션 배포

### 2-2. 비호환(Backward Incompatible) 변경
- 컬럼 삭제/타입 변경
- NOT NULL 강제
- 기존 데이터 변환 필요

권장:
- 다단계 배포(Expand -> Migrate -> Contract) 적용

---

## 3) 표준 배포 순서
1. 변경 설계
- 엔티티 변경점과 SQL 변경점 매핑
- 롤백 SQL 함께 준비

2. 사전 검증(로컬/스테이징 수준)
- 변경 SQL 적용
- 서비스 기동 시 `validate` 통과 확인
- 핵심 API/헬스체크 확인

3. 운영 적용
- DB 마이그레이션 먼저 실행
- 배포 수행(CI/CD 또는 수동)
- `rollout status`/헬스체크/스모크 테스트 수행

4. 사후 검증
- 에러 로그/이벤트 모니터링 30분
- 이상 시 즉시 롤백 판단

---

## 4) SQL 적용 체크리스트
- [ ] 변경 SQL과 롤백 SQL이 함께 준비됨
- [ ] 영향 테이블 row 수/락 영향 검토 완료
- [ ] 트랜잭션/배치 단위 계획 수립
- [ ] 적용 시간대(저트래픽) 승인 완료
- [ ] 적용 후 검증 쿼리 준비 완료

---

## 5) 운영 적용 예시 절차
```bash
# 1) (DB 클라이언트에서) migration SQL 적용
# 예: ALTER TABLE ..., CREATE INDEX ...

# 2) 애플리케이션 배포
kubectl -n thock-prod rollout status deploy/api-gateway --timeout=300s
kubectl -n thock-prod rollout status deploy/member-service --timeout=300s
kubectl -n thock-prod rollout status deploy/product-service --timeout=300s
kubectl -n thock-prod rollout status deploy/market-service --timeout=300s
kubectl -n thock-prod rollout status deploy/payment-service --timeout=300s
kubectl -n thock-prod rollout status deploy/settlement-service --timeout=300s

# 3) validate 실패/스키마 오류 탐지
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 50
kubectl -n thock-prod logs deploy/<문제서비스> --tail=300
```

---

## 6) 롤백 전략
### 6-1. 애플리케이션만 롤백 가능한 경우
- 하위 호환 스키마라면 앱만 이전 태그로 롤백 가능

### 6-2. 스키마 롤백이 필요한 경우
- 비호환 변경이 반영된 경우 롤백 SQL 필요
- 데이터 손실 가능성이 있으면 즉시 수동 승인 절차 수행

원칙:
- 롤백 가능성 없는 파괴적 DDL은 사전 백업 없이 실행 금지

---

## 7) 권장 운영 방식
- 단기: 수동 SQL + 체크리스트 기반 운영
- 중기: Flyway/Liquibase 도입해 마이그레이션 이력 버전화
- 장기: 배포 파이프라인에 "마이그레이션 -> 앱 배포 -> 스모크 테스트" 자동화

---

## 8) 기록 규칙
- 스키마 변경 건은 반드시 `OPS_CHANGELOG.md`에 기록
- 변경 시점/SQL 파일명/영향 범위/롤백 계획을 포함
- 배포 후 점검 결과는 `OPS_CHECK_TEMPLATE.md`에 기록

---

## 개정 이력
| 버전 | 일자 | 수정자 | 변경 요약 |
|---|---|---|---|
| v1.0.0 | 2026-03-05 | ops-admin | ddl-auto=validate 기반 DB 스키마 변경 운영 절차 초안 작성 |
| v1.0.1 | 2026-03-05 | ops-admin | 공통 링크에 신규 정책 문서(보안) 반영 |
| v1.0.2 | 2026-03-09 | ops-admin | 삭제된 모니터링 문서 링크를 Kubernetes 가이드 링크로 대체 |
