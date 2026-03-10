# 운영 변경 이력 (Ops Changelog)

운영 환경에서 발생한 설정/배포/정책 변경 내역을 기록합니다.
목표는 "언제, 무엇을, 왜, 결과가 어땠는지"를 빠르게 추적하는 것입니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.3 |
| 최종 수정일 | 2026-03-09 |
| 수정자 | ops-admin |

---

## 템플릿
### [YYYY-MM-DD HH:mm] 변경 제목
- 변경 유형: `배포` / `설정` / `보안정책` / `장애조치` / `롤백`
- 대상 환경: `kubernetes` / `docker-compose`
- 대상 컴포넌트: (예: api-gateway, ingress, configmap, nginx)
- 작업자:
- 목적/배경:
- 변경 내용:
  - 
  - 
- 검증 방법:
  - 
- 결과:
  - `성공` / `부분성공` / `실패`
  - 상세:
- 영향도:
  - 사용자 영향: `없음` / `부분` / `전체`
  - 장애 등급: `Sev1` / `Sev2` / `Sev3` / `해당없음`
- 롤백 여부:
  - `불필요` / `수행` / `대기`
  - 롤백 상세(수행 시):
- 후속 TODO:
  - 
- 참조:
  - PR:
  - Commit/Image Tag:
  - 이슈:

---

## 변경 이력

### [2026-03-09 00:30] Compose release-state.env 및 Kubernetes 수동 배포 문서 정리
- 변경 유형: `설정`
- 대상 환경: `kubernetes + docker-compose`
- 대상 컴포넌트: 운영 문서, Compose 대체 실행 절차, Kubernetes 수동 배포 절차
- 작업자: ops-admin
- 목적/배경:
  - 선택 배포 기준으로 전환된 뒤 Compose와 Kubernetes 문서에 남은 전체 배포/단일 태그 전제를 제거
- 변경 내용:
  - Compose 문서에 `.env`와 `release-state.env` 분리 운영, `compose.sh` 실행 기준 반영
  - Kubernetes 문서에 `kubectl set image` 기반 서비스별 태그 지정 절차 반영
  - 삭제된 `OPS_MONITORING_ALERTS.md` 참조를 `deploy-kubernetes/README.md` 기준으로 정리
- 검증 방법:
  - 배포/운영 문서 간 절차 대조 검토
  - 구식 스크립트/삭제 문서/단일 IMAGE_TAG 참조 여부 점검
- 결과:
  - `성공`
  - 상세: 운영 문서가 현재 배포 모델과 동일한 기준으로 정렬됨
- 영향도:
  - 사용자 영향: `없음`
  - 장애 등급: `해당없음`
- 롤백 여부:
  - `불필요`
- 후속 TODO:
  - 실제 선택 배포 이슈 종료 시 최종 운영 점검 기록 1건 추가
- 참조:
  - PR:
  - Commit/Image Tag:
  - 이슈: 배포 문서 최신화

### [2026-03-09 00:00] GitHub Actions 선택 서비스 배포 기준 반영
- 변경 유형: `설정`
- 대상 환경: `kubernetes`
- 대상 컴포넌트: GitHub Actions CD, 운영 배포 절차 문서
- 작업자: ops-admin
- 목적/배경:
  - PR merge 시 전체 서비스가 아닌 변경 서비스만 이미지 빌드/배포하도록 CD를 최적화
- 변경 내용:
  - `deploy.yml`에서 PR 변경 파일 기준으로 영향 서비스만 `build/push/set image` 수행
  - `common/`, `gradle/`, `settings.gradle`, `settings.gradle.kts`, `build.gradle`, `build.gradle.kts`, `gradlew`, `gradlew.bat`, `gradle.properties` 변경 시 전체 서비스 배포로 처리
  - `OPERATIONS_RUNBOOK.md`, `OPS_CHECK_TEMPLATE.md`에 선택 배포/검증 기준 반영
- 검증 방법:
  - 워크플로우와 운영 문서 간 절차 대조 검토
  - 선택 배포/전체 배포 분기 조건 확인
- 결과:
  - `성공`
  - 상세: 운영 기준 문서를 현재 CD 동작과 일치하도록 정렬
- 영향도:
  - 사용자 영향: `없음`
  - 장애 등급: `해당없음`
- 롤백 여부:
  - `불필요`
- 후속 TODO:
  - 최초 선택 배포 PR에서 matrix 대상 서비스와 배포 검증 로그 확인
- 참조:
  - PR:
  - Commit/Image Tag:
  - 이슈: 선택 배포 최적화

### [2026-03-05 00:00] 초기 템플릿 생성
- 변경 유형: `설정`
- 대상 환경: `kubernetes + docker-compose`
- 대상 컴포넌트: 운영 문서
- 작업자: (작성자 기입)
- 목적/배경:
  - 운영 변경 추적 표준화
- 변경 내용:
  - `AWS EC2/OPS_CHANGELOG.md` 파일 생성
- 검증 방법:
  - 파일 생성 및 내용 확인
- 결과:
  - `성공`
  - 상세: 템플릿 작성 완료
- 영향도:
  - 사용자 영향: `없음`
  - 장애 등급: `해당없음`
- 롤백 여부:
  - `불필요`
- 후속 TODO:
  - 실제 운영 변경 건부터 지속 기록
- 참조:
  - PR:
  - Commit/Image Tag:
  - 이슈:

---

## 작성 예시

### [2026-03-05 10:40] Kubernetes 전 서비스 이미지 태그 배포
- 변경 유형: `배포`
- 대상 환경: `kubernetes`
- 대상 컴포넌트: api-gateway, member/product/market/payment/settlement-service
- 작업자: honggildong
- 목적/배경:
  - CI 배포 산출물(merge commit SHA) 운영 반영
- 변경 내용:
  - `kubectl set image`로 6개 서비스 태그를 `8b1f8af1710be9b9f6718fe36cdd1bd1ca3ef45f`로 변경
  - `rollout status`로 전체 배포 완료 확인
- 검증 방법:
  - `kubectl -n thock-prod get pods`
  - `kubectl -n thock-prod get deploy ... -o=jsonpath=...` (이미지 태그 확인)
  - 내부 헬스체크(`/actuator/health`) 5개 서비스 점검
- 결과:
  - `성공`
  - 상세: 모든 Pod `READY 1/1`, 헬스체크 `UP`
- 영향도:
  - 사용자 영향: `없음`
  - 장애 등급: `해당없음`
- 롤백 여부:
  - `불필요`
- 후속 TODO:
  - 배포 직후 30분 모니터링
- 참조:
  - PR: #123
  - Commit/Image Tag: `8b1f8af1710be9b9f6718fe36cdd1bd1ca3ef45f`
  - 이슈: 배포 파이프라인 태그 고정 정책

### [2026-03-05 16:15] Swagger/Grafana/Redpanda 관리자 IP 화이트리스트 적용
- 변경 유형: `보안정책`
- 대상 환경: `kubernetes + docker-compose`
- 대상 컴포넌트: ingress, middleware, nginx
- 작업자: honggildong
- 목적/배경:
  - 운영 관리 경로의 외부 무단 접근 차단
- 변경 내용:
  - Kubernetes: `ip-whitelist-middleware.yaml`, `swagger/grafana/redpanda ingress` 분리 적용
  - Compose: nginx 경로(`/swagger-ui`, `/grafana/`, `/redpanda/`)에 `allow/deny` 적용
- 검증 방법:
  - 허용 IP에서 `curl -Ik https://api.thock.site/swagger-ui.html` => `200/302`
  - 비허용 IP(LTE)에서 동일 요청 => `403`
- 결과:
  - `성공`
  - 상세: 관리자 경로 모두 정책대로 동작
- 영향도:
  - 사용자 영향: `없음` (관리자 경로만 제한)
  - 장애 등급: `해당없음`
- 롤백 여부:
  - `불필요`
- 후속 TODO:
  - 관리자 IP 변경 절차 문서화
- 참조:
  - PR: #124
  - Commit/Image Tag: ingress config commit `abcd1234`
  - 이슈: 운영 경로 접근제어

### [2026-03-06 09:20] startupProbe 타이밍 조정 후 일부 서비스 롤백
- 변경 유형: `장애조치`
- 대상 환경: `kubernetes`
- 대상 컴포넌트: member-service, payment-service
- 작업자: honggildong
- 목적/배경:
  - 배포 직후 `Startup probe failed` 반복 발생
- 변경 내용:
  - `startupProbe.initialDelaySeconds`를 30으로 상향 적용
  - payment-service는 여전히 불안정하여 직전 ReplicaSet으로 롤백
- 검증 방법:
  - `kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 50`
  - `kubectl -n thock-prod rollout status deploy/payment-service`
  - `kubectl -n thock-prod top pod`
- 결과:
  - `부분성공`
  - 상세: member-service 안정화, payment-service는 롤백 후 정상
- 영향도:
  - 사용자 영향: `부분`
  - 장애 등급: `Sev2`
- 롤백 여부:
  - `수행`
  - 롤백 상세(수행 시): `kubectl -n thock-prod rollout undo deploy/payment-service`
- 후속 TODO:
  - payment-service 기동 지연 원인 분석(JPA 초기화/DB 연결 시간)
- 참조:
  - PR: #125
  - Commit/Image Tag: `f0e1d2c3...` (롤백 대상)
  - 이슈: Probe 튜닝

---

## 개정 이력
| 버전 | 일자 | 수정자 | 변경 요약 |
|---|---|---|---|
| v1.0.0 | 2026-03-05 | ops-admin | 변경 이력 템플릿 및 작성 예시 추가 |
| v1.0.1 | 2026-03-05 | ops-admin | 공통 링크에 신규 정책 문서(보안) 반영 |
| v1.0.2 | 2026-03-09 | ops-admin | 선택 서비스 배포 기준 운영 변경 이력 추가 |
| v1.0.3 | 2026-03-09 | ops-admin | Compose/Kubernetes 최신 운영 방식 기준 문서 정리 이력 추가 |
