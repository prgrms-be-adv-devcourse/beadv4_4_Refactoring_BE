# 운영 모니터링/알람 기준서 (Ops Monitoring & Alerts)

운영 환경에서 관찰해야 할 핵심 지표, 임계치, 알람 발생 시 1차 대응 절차를 정의합니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.0 |
| 최종 수정일 | 2026-03-05 |
| 수정자 | ops-admin |

## 빠른 검색 키워드
`monitoring`, `alerts`, `threshold`, `error-rate`, `latency`, `cpu`, `memory`, `pod-restart`, `slo`, `triage`

---

## 1) 목적
- 장애를 "늦게 발견"하는 리스크를 줄입니다.
- 경고/치명 알람을 구분해 대응 우선순위를 명확히 합니다.
- 알람 발생 시 운영자가 1분 내 초기 조치를 시작할 수 있게 합니다.

---

## 2) 핵심 지표 및 임계치 (기본값)
| 분류 | 지표 | 경고(Warning) | 치명(Critical) | 확인 명령/위치 |
|---|---|---|---|---|
| 가용성 | 핵심 Pod 상태 | `READY!=1/1` 1개 이상 3분 지속 | `CrashLoopBackOff/ImagePullBackOff` 즉시 | `kubectl -n thock-prod get pods` |
| 가용성 | 재시작 횟수 | 단일 Pod 재시작 3회/10분 | 다수 Pod 재시작/롤링 루프 | `kubectl -n thock-prod get pods` |
| API 품질 | 5xx 비율 | 1% 이상 5분 | 3% 이상 5분 | API Gateway 로그/대시보드 |
| API 품질 | p95 지연 | 1s 초과 5분 | 3s 초과 5분 | API Gateway/APM |
| 리소스 | CPU 사용률 | Pod 평균 80% 이상 10분 | Pod 95% 이상 10분 | `kubectl -n thock-prod top pod` |
| 리소스 | 메모리 사용률 | Pod 80% 이상 10분 | Pod 90% 이상 10분 또는 OOMKill | `kubectl -n thock-prod top pod` |
| 인프라 | 디스크 사용률 | 70% 이상 | 80% 이상(즉시 조치), 90% 이상(Sev2+) | `df -h` |
| 데이터 | DB 연결 실패 | 간헐적 연결 실패 로그 | 다수 서비스 동시 DB 연결 실패 | 각 서비스 로그 |
| 메시징 | Kafka/Redpanda 지연 | Consumer lag 증가 추세 | lag 급증 + 처리 정지 | Redpanda Console |

참고:
- 임계치는 초기 기준입니다. 운영 데이터 축적 후 서비스 특성에 맞게 조정합니다.

---

## 3) 알람 등급별 대응
### 3-1. Warning
- 5~15분 내 원인 파악
- 재현/확산 여부 확인
- 필요 시 `OPS_CHANGELOG.md`에 관찰 메모 기록

### 3-2. Critical
- 즉시 Sev2 이상으로 대응
- `OPERATIONS_RUNBOOK.md`의 `11. 장애 의심 시 1분 트리아지` 실행
- 최근 배포 변경점이 원인으로 의심되면 즉시 롤백 검토

---

## 4) 알람 발생 시 1차 조치(1분)
```bash
# 1) 상태
kubectl -n thock-prod get pods -o wide

# 2) 최근 이벤트
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 40

# 3) 문제 서비스 로그
kubectl -n thock-prod logs deploy/api-gateway --tail=200
kubectl -n thock-prod logs deploy/<문제서비스> --tail=200

# 4) 리소스
kubectl -n thock-prod top pod
```

---

## 5) 알람 운영 규칙
- 경고 알람은 누적 추세 중심으로 판단합니다.
- 치명 알람은 단일 신호라도 서비스 영향이 명확하면 즉시 대응합니다.
- 알람 해제 후에도 30분 관찰을 유지합니다.
- 튜닝/임계치 변경은 PR로 관리하고 `OPS_CHANGELOG.md`에 기록합니다.

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
| v1.0.0 | 2026-03-05 | ops-admin | 모니터링 지표/임계치/알람 대응 기준 초안 작성 |
| v1.0.1 | 2026-03-05 | ops-admin | 공통 링크에 신규 정책 문서(보안) 반영 |
