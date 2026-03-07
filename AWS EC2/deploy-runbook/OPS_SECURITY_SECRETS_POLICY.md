# 운영 비밀정보/접근권한 정책 (Ops Security & Secrets Policy)

운영 환경의 비밀정보(Secret) 관리, 접근권한 통제, 유출 대응 절차를 정의합니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.0 |
| 최종 수정일 | 2026-03-05 |
| 수정자 | ops-admin |

## 빠른 검색 키워드
`secrets`, `security`, `credential-rotation`, `access-control`, `least-privilege`, `incident-response`, `token`, `password`, `key-rotation`

---

## 1) 목적과 범위
- 목적:
  - 운영 비밀정보 유출/오용 리스크 최소화
  - 권한 관리 표준화
  - 보안 사고 발생 시 대응 시간 단축
- 범위:
  - Kubernetes Secret, Docker Compose `.env`, DB 계정/비밀번호, JWT/내부 통신 키

---

## 2) 기본 원칙
- 최소 권한(Least Privilege) 원칙 적용
- 비밀정보는 Git 저장소에 평문 커밋 금지
- 운영/개발 비밀정보 분리
- 비밀정보는 정기 회전(rotate) 수행
- 권한 변경/회수는 즉시 반영

---

## 3) 비밀정보 분류
1. 인증/인가 키
- JWT 시크릿, 내부 서비스 인증 키(`X-Gateway-Auth`, `X-Internal-Auth`)

2. DB 자격증명
- 서비스별 DB 사용자/비밀번호

3. 외부 연동 자격증명
- 결제/메시징/서드파티 API 키

4. 인프라 접근정보
- 서버 SSH 접근키, 관리자 계정

---

## 4) 저장 및 주입 정책
### 4-1. Kubernetes
- Secret은 `secrets.yaml`로 관리하되 저장소 커밋 금지
- Pod에는 `envFrom.secretRef` 또는 `valueFrom.secretKeyRef`로 주입
- Secret 변경 후 rollout restart 및 동작 검증 필수

### 4-2. Docker Compose
- 민감값은 `.env`로 주입하되 저장소 커밋 금지
- 서버에만 보관, 권한 제한(`chmod 600`) 권장
- 값 변경 후 `docker compose down && docker compose up -d` 및 로그 검증

---

## 5) 회전(Rotation) 정책
- 주기(권장):
  - JWT/내부 인증 키: 90일
  - DB 비밀번호: 90일
  - 외부 연동 키: 공급자 정책 또는 90일
  - SSH 접근키: 180일
- 회전 절차:
  1. 신규 값 발급
  2. 서비스 설정 반영
  3. 무중단 배포/재기동
  4. 정상 동작 검증
  5. 구 키 폐기
  6. `OPS_CHANGELOG.md` 기록

---

## 6) 접근권한 관리 정책
- 운영 계정은 개인 계정 기반으로 부여(공용 계정 최소화)
- 권한 요청/승인은 이력으로 남김
- 퇴사/역할 변경 시 당일 권한 회수
- 긴급 권한 부여는 기간 제한(TTL) 후 자동/수동 회수

---

## 7) 유출 의심/사고 대응 절차
1. 즉시 조치
- 노출된 키/비밀번호 즉시 폐기 및 재발급
- 의심 트래픽 차단(필요 시 IP/경로 차단)
- 영향 서비스 임시 보호 모드(읽기전용/일시차단) 검토

2. 원인 분석
- 변경 이력(`OPS_CHANGELOG.md`) 확인
- 접근 로그/애플리케이션 로그 확인
- 노출 범위(어떤 키, 어떤 시스템) 식별

3. 복구/재발 방지
- 관련 Secret 전면 회전
- 권한 정책 보강
- 문서/운영 절차 보완

---

## 8) 운영 체크리스트
- [ ] 민감정보 파일이 Git에 커밋되지 않았는가
- [ ] Secret 변경 후 rollout/재기동 및 헬스체크를 수행했는가
- [ ] 구 키 폐기를 완료했는가
- [ ] 변경 이력을 `OPS_CHANGELOG.md`에 남겼는가

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
| v1.0.0 | 2026-03-05 | ops-admin | 운영 비밀정보/접근권한 정책 문서 초안 작성 |
| v1.0.1 | 2026-03-05 | ops-admin | 공통 링크 포맷 통일 및 자기참조 링크 추가 |
