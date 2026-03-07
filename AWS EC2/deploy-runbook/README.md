# AWS EC2 운영 문서 인덱스

이 디렉터리는 배포 서버(EC2) 기준 운영 문서를 모아둔 공간입니다.
아래 순서대로 보면 배포/장애/보안 대응 속도를 높일 수 있습니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.0 |
| 최종 수정일 | 2026-03-05 |
| 수정자 | ops-admin |

## 빠른 검색 키워드
`index`, `guide-map`, `entrypoint`, `runbook`, `onboarding`, `check-template`, `changelog`, `aliases`, `ingress-guide`

---

## 1) 가장 먼저 볼 문서
- 운영 표준 절차: `OPERATIONS_RUNBOOK.md`
- 변경 이력 기록: `OPS_CHANGELOG.md`
- 점검 결과 기록: `OPS_CHECK_TEMPLATE.md`
- 모니터링/알람 기준: `OPS_MONITORING_ALERTS.md`
- DB 스키마 변경 가이드: `OPS_DB_MIGRATION_GUIDE.md`
- 문서 작성/최신화 가이드: `OPS_DOCUMENTATION_GUIDE.md`
- 운영 alias 가이드: `OPS_ALIASES.md`
- 운영 온보딩 체크리스트: `OPS_ONBOARDING.md`
- 운영 비밀정보/권한 정책: `OPS_SECURITY_SECRETS_POLICY.md`

---

## 2) 문서 상태 대시보드
| 문서명 | 목적 | 마지막 업데이트일 | 담당자 | 상태 |
|---|---|---|---|---|
| `OPERATIONS_RUNBOOK.md` | 운영 표준 절차/검증/장애 대응 기준 | 2026-03-05 | ops-admin | Active |
| `OPS_CHANGELOG.md` | 운영 변경 이력 기록 | 2026-03-05 | ops-admin | Active |
| `OPS_CHECK_TEMPLATE.md` | 배포/장애 후 점검 결과 템플릿 | 2026-03-05 | ops-admin | Active |
| `OPS_MONITORING_ALERTS.md` | 모니터링 지표/임계치/알람 대응 기준 | 2026-03-05 | ops-admin | Active |
| `OPS_DB_MIGRATION_GUIDE.md` | `ddl-auto=validate` 기준 DB 스키마 변경 절차 | 2026-03-05 | ops-admin | Active |
| `OPS_DOCUMENTATION_GUIDE.md` | 운영 문서 작성/최신화 규칙 | 2026-03-05 | ops-admin | Active |
| `OPS_ALIASES.md` | 운영 명령어 단축/표준화 | 2026-03-05 | ops-admin | Active |
| `OPS_ONBOARDING.md` | 신규 운영자 온보딩 절차 | 2026-03-05 | ops-admin | Active |
| `OPS_SECURITY_SECRETS_POLICY.md` | 비밀정보/접근권한 관리 정책 | 2026-03-05 | ops-admin | Active |
| `deploy-kubernetes/ingress/INGRESS_GUIDE.md` | Ingress/Middleware 적용 순서 | 2026-03-05 | ops-admin | Active |
| `deploy-kubernetes/DEPLOYMENT_CAUTIONS.txt` | Kubernetes 수동 배포 주의사항 | 2026-03-05 | ops-admin | Active |
| `deploy-docker-compose/DEPLOYMENT_CAUTIONS.txt` | Compose 수동 배포 주의사항 | 2026-03-05 | ops-admin | Active |

상태 기준:
- `Active`: 현재 운영 기준으로 유효
- `Review Needed`: 최근 변경 반영 필요

---

## 3) 상황별 진입 가이드

### A. 배포 직후 점검
1. `OPERATIONS_RUNBOOK.md`
2. `10. 배포 직후 5분 체크리스트`
3. `15. 배포 후 자동 검증 스모크 테스트`
4. 점검 결과는 `OPS_CHECK_TEMPLATE.md`로 기록

### B. 장애 발생/의심
1. `OPERATIONS_RUNBOOK.md`
2. `11. 장애 의심 시 1분 트리아지`
3. `13. 장애 대응 우선순위 (Sev 기준)`
4. 결과/조치 내역은 `OPS_CHANGELOG.md`에 기록

### C. 보안 경로(IP 화이트리스트) 변경
1. `deploy-kubernetes/ingress/INGRESS_GUIDE.md`
2. `OPERATIONS_RUNBOOK.md`의 `5. Ingress 변경 표준 절차`
3. 적용 후 `6. 관리자 경로 보안 검증` 수행

### D. Kubernetes/Compose 전환
1. `OPERATIONS_RUNBOOK.md`의 `4. Kubernetes ↔ Compose 전환`
2. 전환 후 `1. 사전 점검`, `2-4. 필수 검증` 재실행

---

## 4) 문서별 용도 요약
- `OPERATIONS_RUNBOOK.md`
  - 운영 표준 절차의 기준 문서
  - 배포/롤백/전환/보안/트러블슈팅/프로브/로그 정책 포함

- `OPS_CHECK_TEMPLATE.md`
  - 배포 직후 점검 결과를 `OK/NOK`로 기록하는 템플릿

- `OPS_CHANGELOG.md`
  - 운영 변경 사항(언제/무엇/왜/결과)을 누적 기록

- `OPS_MONITORING_ALERTS.md`
  - 핵심 지표/임계치/알람 발생 시 1차 대응 기준

- `OPS_DB_MIGRATION_GUIDE.md`
  - `ddl-auto=validate` 환경의 DB 스키마 변경/롤백 절차

- `OPS_DOCUMENTATION_GUIDE.md`
  - 문서 최신화 기준, 버전/개정 이력 작성 규칙

- `OPS_ALIASES.md`
  - 운영 명령어 alias 표준 및 적용 방법

- `OPS_ONBOARDING.md`
  - 신규 운영자 첫날 체크리스트 및 필수 검증 절차

- `OPS_SECURITY_SECRETS_POLICY.md`
  - 비밀정보 회전, 접근권한 통제, 유출 대응 정책

- `deploy-kubernetes/ingress/INGRESS_GUIDE.md`
  - ingress/middleware 적용 순서 및 운영 규칙

- `deploy-kubernetes/DEPLOYMENT_CAUTIONS.txt`
  - Kubernetes 수동 배포 주의사항

- `deploy-docker-compose/DEPLOYMENT_CAUTIONS.txt`
  - Docker Compose 수동 배포 주의사항

---

## 5) 운영 기록 원칙
- 모든 운영 변경은 `OPS_CHANGELOG.md`에 기록
- 모든 점검 결과는 `OPS_CHECK_TEMPLATE.md`로 남김
- 배포/보안 정책 변경 전후에 `OPERATIONS_RUNBOOK.md` 기준 검증 수행

---

## 6) 문서 최신화 체크 절차
### 6-1. 주 1회 점검 (권장: 매주 월요일)
- 문서 상태 대시보드의 `마지막 업데이트일`, `상태` 점검
- 최근 1주 `OPS_CHANGELOG.md`와 문서 내용 불일치 여부 확인
- 불일치 문서는 상태를 `Review Needed`로 변경

### 6-2. 변경 발생 시 즉시 갱신 문서
- 배포/롤백 절차 변경: `OPERATIONS_RUNBOOK.md`, `OPS_CHANGELOG.md`
- 보안/Ingress 정책 변경: `OPERATIONS_RUNBOOK.md`, `deploy-kubernetes/ingress/INGRESS_GUIDE.md`, `OPS_CHANGELOG.md`
- 점검 항목 변경: `OPS_CHECK_TEMPLATE.md`
- 모니터링/임계치 변경: `OPS_MONITORING_ALERTS.md`
- DB 스키마 변경 정책 변경: `OPS_DB_MIGRATION_GUIDE.md`
- 문서 작성 규칙/체계 변경: `OPS_DOCUMENTATION_GUIDE.md`
- 운영 명령어 변경: `OPS_ALIASES.md`
- 온보딩 절차 변경: `OPS_ONBOARDING.md`
- 비밀정보/권한 정책 변경: `OPS_SECURITY_SECRETS_POLICY.md`

### 6-3. 책임 규칙 (Who / When / How)
- Who: 변경 작업 수행자(또는 리뷰어)가 문서 업데이트 책임
- When: 운영 변경 완료 직후(당일) 반영
- How:
  - 문서 메타정보의 `최종 수정일` 갱신
  - `개정 이력` 1줄 추가
  - `README.md`를 수정한 경우에도 본 문서 `개정 이력`에 1줄 추가를 필수로 수행
  - 필요 시 `문서 상태 대시보드` 업데이트

---

## 7) 관련 문서 (공통 링크)
- `AWS EC2/README.md`
- `AWS EC2/OPERATIONS_RUNBOOK.md`
- `AWS EC2/OPS_CHECK_TEMPLATE.md`
- `AWS EC2/OPS_CHANGELOG.md`
- `AWS EC2/OPS_ONBOARDING.md`
- `AWS EC2/OPS_ALIASES.md`
- `AWS EC2/OPS_MONITORING_ALERTS.md`
- `AWS EC2/OPS_DB_MIGRATION_GUIDE.md`
- `AWS EC2/OPS_DOCUMENTATION_GUIDE.md`
- `AWS EC2/OPS_SECURITY_SECRETS_POLICY.md`

---

## 개정 이력
| 버전 | 일자 | 수정자 | 변경 요약 |
|---|---|---|---|
| v1.0.0 | 2026-03-05 | ops-admin | 운영 문서 인덱스 구성 및 문서 연결 표준화 |
| v1.0.1 | 2026-03-05 | ops-admin | 신규 정책 문서(모니터링/DB/보안) 연계 및 최신화 규칙 보강 |
| v1.0.2 | 2026-03-05 | ops-admin | 운영 문서 작성/최신화 가이드 문서 연계 추가 |
