# 운영 점검 결과 템플릿

이 문서는 배포 직후/장애 대응 후 운영 점검 결과를 일관되게 기록하기 위한 템플릿입니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.0 |
| 최종 수정일 | 2026-03-05 |
| 수정자 | ops-admin |

## 빠른 검색 키워드
`check-template`, `post-deploy-check`, `ops-checklist`, `ok-nok`, `health-check`, `verification`, `release-check`

---

## 1) 기본 정보
- 점검 일시:
- 점검자:
- 환경: `kubernetes` / `docker-compose`
- 배포 태그(이미지 SHA):
- 관련 PR/이슈:

---

## 2) 핵심 체크 결과 (OK / NOK)
- [ ] 롤아웃 성공 (`kubectl rollout status`)
- [ ] 핵심 Pod 상태 정상 (`READY 1/1`, `Running`)
- [ ] 실행 이미지 태그 일치
- [ ] 내부 헬스체크 모두 `UP`
- [ ] 외부 핵심 경로 응답 정상
- [ ] 관리자 경로 IP 정책 정상 (`swagger/grafana/redpanda`)
- [ ] 리소스 사용량 이상 없음 (`kubectl top pod`)
- [ ] 이벤트 로그 치명 경고 없음

요약 판정: `OK` / `NOK`

---

## 3) 실행 명령어 및 주요 결과
### 3-1. 실행한 명령어
```bash
# 예시
kubectl -n thock-prod get pods
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 30
kubectl -n thock-prod top pod
```

### 3-2. 핵심 결과 요약
- 
- 
- 

---

## 4) 발견 이슈
| 번호 | 증상 | 영향도 (Sev1/2/3) | 원인 추정 | 상태 |
|---|---|---|---|---|
| 1 |  |  |  | Open/In Progress/Resolved |

---

## 5) 조치 내역
| 시간 | 조치 내용 | 실행자 | 결과 |
|---|---|---|---|
|  |  |  |  |

---

## 6) 후속 TODO
| 번호 | TODO | 담당자 | 목표 일자 | 상태 |
|---|---|---|---|---|
| 1 |  |  |  | Todo/Doing/Done |

---

## 7) 최종 결론
- 서비스 오픈 가능 여부: `가능` / `보류`
- 보류 사유(있을 경우):
- 다음 점검 예정 시각:

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
| v1.0.0 | 2026-03-05 | ops-admin | 운영 점검 기록 템플릿 초기 표준화 |
| v1.0.1 | 2026-03-05 | ops-admin | 공통 링크에 신규 정책 문서(보안) 반영 |
