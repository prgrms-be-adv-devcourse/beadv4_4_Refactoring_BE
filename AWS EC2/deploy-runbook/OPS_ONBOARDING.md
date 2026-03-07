# 운영 온보딩 체크리스트 (Ops Onboarding)

신규 운영자가 첫날 수행해야 하는 필수 설정/검증 절차입니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.0 |
| 최종 수정일 | 2026-03-05 |
| 수정자 | ops-admin |

## 빠른 검색 키워드
`onboarding`, `first-day`, `ops-init`, `k8s-check`, `compose-check`, `security-check`, `handover`, `new-operator`

---

## 1) 접근 및 기본 환경 확인
- [ ] EC2 SSH 접속 가능
- [ ] 현재 사용자/권한 확인
- [ ] 필수 명령어 동작 확인 (`kubectl`, `docker`, `docker compose`)

```bash
whoami
hostname
kubectl version --client
docker --version
docker compose version
```

---

## 2) 운영 문서 위치 확인
- [ ] 운영 인덱스 문서 확인: `AWS EC2/README.md`
- [ ] 런북 확인: `AWS EC2/OPERATIONS_RUNBOOK.md`
- [ ] 변경 이력 템플릿 확인: `AWS EC2/OPS_CHANGELOG.md`
- [ ] 점검 템플릿 확인: `AWS EC2/OPS_CHECK_TEMPLATE.md`
- [ ] alias 가이드 확인: `AWS EC2/OPS_ALIASES.md`

---

## 3) 현재 실행 모드 확인 (K8s / Compose)
- [ ] k3s 상태 확인
- [ ] compose 컨테이너 상태 확인
- [ ] 동시 실행 여부 점검

```bash
sudo systemctl status k3s
docker ps
```

판정:
- 운영 기본은 Kubernetes
- Compose는 대체/점검용
- 두 환경 동시 운영 금지

---

## 4) Kubernetes 기본 점검
- [ ] 노드 상태 정상 (`Ready`)
- [ ] 핵심 Pod 상태 정상 (`READY 1/1`, `Running`)
- [ ] 최근 이벤트 치명 경고 없음
- [ ] 리소스 사용량 확인

```bash
kubectl get nodes
kubectl -n thock-prod get pods -o wide
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 40
kubectl -n thock-prod top pod
```

---

## 5) 서비스 연결/헬스체크 확인
- [ ] api-gateway에서 내부 서비스 헬스 응답 확인
- [ ] 외부 경로 접근 정책 확인

```bash
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://member-service:8081/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://product-service:8082/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://market-service:8083/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://payment-service:8084/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://settlement-service:8085/actuator/health'

curl -Ik https://api.thock.site/swagger-ui.html
curl -Ik https://api.thock.site/grafana/
curl -Ik https://api.thock.site/redpanda/
```

---

## 6) 배포 운영 규칙 숙지
- [ ] `latest` 고정 태그 배포 금지(운영은 SHA 태그 사용)
- [ ] 수동 배포 시 이미지 태그 명시 (`kubectl set image`)
- [ ] 배포 후 `rollout status` + 스모크 테스트 필수
- [ ] 변경 사항은 `OPS_CHANGELOG.md`에 기록

---

## 7) 보안 정책 숙지
- [ ] Swagger/Grafana/Redpanda 관리자 IP 제한 정책 이해
- [ ] Ingress 적용 순서 문서 확인
- [ ] Middleware 선적용 원칙 이해

참고:
- `AWS EC2/deploy-kubernetes/ingress/INGRESS_GUIDE.md`

---

## 8) 첫날 완료 보고 템플릿
- 점검 일시:
- 점검자:
- 실행 모드: `kubernetes` / `docker-compose`
- 이상 여부: `정상` / `주의 필요`
- 발견 이슈:
- 후속 TODO:
- 보고 링크(문서/이슈):

---

## 9) 권장 다음 단계
- [ ] `OPS_ALIASES.md` 기준 alias 적용
- [ ] 최근 1주 `OPS_CHANGELOG.md` 읽고 변경 흐름 파악
- [ ] 장애 대응 모의 점검 1회 수행 (`Sev2` 시나리오 기준)

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
| v1.0.0 | 2026-03-05 | ops-admin | 신규 운영자 온보딩 체크리스트 표준화 |
| v1.0.1 | 2026-03-05 | ops-admin | 공통 링크에 신규 정책 문서(보안) 반영 |
