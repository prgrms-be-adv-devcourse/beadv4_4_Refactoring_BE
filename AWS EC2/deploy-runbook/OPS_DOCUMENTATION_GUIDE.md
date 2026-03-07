# 운영 문서 작성/최신화 가이드 (Ops Documentation Guide)

신규/기존 운영자가 문서를 일관된 방식으로 수정하기 위한 기준 문서입니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.0 |
| 최종 수정일 | 2026-03-05 |
| 수정자 | ops-admin |

## 빠른 검색 키워드
`documentation`, `doc-guide`, `update-rules`, `revision-history`, `versioning`, `owner`, `runbook-maintenance`

---

## 1) 언제 어떤 문서를 수정해야 하나
### 1-1. 배포/롤백 절차 변경
- 수정 문서:
  - `OPERATIONS_RUNBOOK.md`
  - `OPS_CHANGELOG.md`
  - 필요 시 `OPS_CHECK_TEMPLATE.md`

### 1-2. 보안/Ingress/IP 정책 변경
- 수정 문서:
  - `OPERATIONS_RUNBOOK.md`
  - `deploy-kubernetes/ingress/INGRESS_GUIDE.md`
  - `OPS_SECURITY_SECRETS_POLICY.md`
  - `OPS_CHANGELOG.md`

### 1-3. 모니터링/알람 임계치 변경
- 수정 문서:
  - `OPS_MONITORING_ALERTS.md`
  - `OPERATIONS_RUNBOOK.md` (필요 시)
  - `OPS_CHANGELOG.md`

### 1-4. DB 스키마 운영 정책 변경
- 수정 문서:
  - `OPS_DB_MIGRATION_GUIDE.md`
  - `OPERATIONS_RUNBOOK.md` (검증 절차 영향 시)
  - `OPS_CHANGELOG.md`

### 1-5. 신규 운영자 절차 변경
- 수정 문서:
  - `OPS_ONBOARDING.md`
  - `README.md` (인덱스/용도 요약)

---

## 2) 문서 수정 시 필수 체크리스트
- [ ] 문서 본문 변경
- [ ] 문서 메타정보의 `최종 수정일` 갱신
- [ ] 해당 문서 `개정 이력` 1줄 추가
- [ ] `README.md` 문서 상태 대시보드(필요 시) 갱신
- [ ] 연관 문서의 `관련 문서(공통 링크)` 누락 여부 확인
- [ ] 운영 변경이면 `OPS_CHANGELOG.md`에 변경 건 기록

---

## 3) 버전/개정 이력 작성 기준
### 3-1. 버전 규칙 (문서 내부용)
- `v1.0.1` 형태 사용
- 권장 기준:
  - Patch(`+0.0.1`): 오탈자, 문구 명확화, 링크 수정
  - Minor(`+0.1.0`): 섹션 추가, 체크리스트 확장
  - Major(`+1.0.0`): 문서 구조/운영 정책 대개편

### 3-2. 개정 이력 작성 예시
| 버전 | 일자 | 수정자 | 변경 요약 |
|---|---|---|---|
| v1.0.1 | 2026-03-05 | honggildong | 보안 정책 섹션 문구 보강 및 링크 수정 |

---

## 4) README 최신화 규칙
- 아래 항목 변경 시 `README.md`를 함께 수정:
  - 새 문서 추가/삭제
  - 문서 상태 변경(`Active`, `Review Needed`)
  - 문서 목적/우선순위 변경

필수 반영 위치:
1. `1) 가장 먼저 볼 문서`
2. `2) 문서 상태 대시보드`
3. `4) 문서별 용도 요약`
4. `7) 관련 문서 (공통 링크)`
5. `개정 이력`

---

## 5) 빠른 실무 절차 (3분)
1. 변경 대상 문서 수정
2. `최종 수정일` + `개정 이력` 업데이트
3. `README` 반영 필요 여부 확인
4. `OPS_CHANGELOG`에 운영 변경 기록
5. PR 설명에 "문서 최신화 반영" 체크

---

## 6) 자주 하는 실수
- 본문만 바꾸고 개정 이력을 누락
- README 대시보드 업데이트 누락
- 공통 링크 누락으로 문서 탐색 단절
- 운영 변경이 있었는데 `OPS_CHANGELOG` 미기록

---

## 관련 문서 (공통 링크)
- `AWS EC2/README.md`
- `AWS EC2/OPERATIONS_RUNBOOK.md`
- `AWS EC2/OPS_CHECK_TEMPLATE.md`
- `AWS EC2/OPS_CHANGELOG.md`
- `AWS EC2/OPS_ONBOARDING.md`
- `AWS EC2/OPS_ALIASES.md`
- `AWS EC2/OPS_MONITORING_ALERTS.md`
- `AWS EC2/OPS_DB_MIGRATION_GUIDE.md`
- `AWS EC2/OPS_SECURITY_SECRETS_POLICY.md`
- `AWS EC2/OPS_DOCUMENTATION_GUIDE.md`

---

## 개정 이력
| 버전 | 일자 | 수정자 | 변경 요약 |
|---|---|---|---|
| v1.0.0 | 2026-03-05 | ops-admin | 운영 문서 작성/최신화 가이드 초안 작성 |

