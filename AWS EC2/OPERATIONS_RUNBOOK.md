# Thock Operations Runbook

이 문서는 운영자가 실제 배포 서버에서 따라야 하는 표준 실행 절차를 정리합니다.
대상 환경은 단일 EC2 + K3s(기본) + Docker Compose(대체)입니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.0 |
| 최종 수정일 | 2026-03-05 |
| 수정자 | ops-admin |

## 빠른 검색 키워드
`runbook`, `kubernetes`, `docker-compose`, `rollout`, `rollback`, `ingress`, `probe`, `smoke-test`, `troubleshooting`, `severities`

## 0. 공통 원칙
- 운영 기본 실행 환경은 Kubernetes(K3s)입니다.
- Docker Compose는 점검/대체 실행 시에만 사용합니다.
- 앱 이미지는 `latest` 대신 고정 태그(SHA)를 사용합니다.
- 수동 배포 시 태그를 반드시 명시합니다.
- 설정 변경(ingress/configmap/secret) 후에는 필요한 재시작/검증을 반드시 수행합니다.

## 0-1. 경로 보안 정책 표 (공통 기준)
| 경로 | 접근 정책 | 허용 조건 | 비허용 기대 코드 |
|---|---|---|---|
| `/swagger-ui.html`, `/swagger-ui/*` | 관리자 전용 | 관리자 IP(필요 시 + 인증) | `403` |
| `/v3/api-docs*`, `/*-service/v3/api-docs` | 관리자 전용 | 관리자 IP(필요 시 + 인증) | `403` |
| `/grafana`, `/grafana/*` | 관리자 전용 | 관리자 IP(필요 시 + 인증) | `403` |
| `/redpanda`, `/redpanda/*` | 관리자 전용 | 관리자 IP(필요 시 + 인증) | `403` |
| `/actuator/*` (외부 진입 경로) | 외부 차단 | 없음 | `403` 또는 `404` |
| `/api/v1/*` | 서비스 정책 적용 | JWT/권한 정책 충족 | 정책별 상이 (`200/401/403`) |

정책 운영 규칙:
- 정책 변경 시 Kubernetes(Traefik)와 Compose(Nginx)를 동시에 수정합니다.
- 허용/비허용 IP 테스트는 두 환경 모두에서 동일하게 수행합니다.

## 1. 사전 점검
```bash
# 현재 실행 모드 확인
sudo systemctl status k3s
docker ps

# k8s 노드/파드 상태
kubectl get nodes
kubectl -n thock-prod get pods
```

판정:
- K3s 운영 시: `k3s active`, 핵심 서비스 Pod가 `READY 1/1`
- Compose 운영 시: `docker ps`에 서비스 컨테이너가 모두 `Up`

---

## 2. 정상 배포 (Kubernetes, 권장)

### 2-1. CI/CD 자동 배포
- PR merge 후 GitHub Actions가 이미지 빌드/배포를 수행합니다.
- 배포 후 아래 검증만 수행합니다.

### 2-2. 수동 배포 (이미지 태그 지정 필수)
```bash
IMAGE_TAG=<배포할_SHA>
NS=thock-prod

kubectl -n $NS set image deployment/api-gateway api-gateway=sang234/api-gateway:$IMAGE_TAG
kubectl -n $NS set image deployment/member-service member-service=sang234/member-service:$IMAGE_TAG
kubectl -n $NS set image deployment/product-service product-service=sang234/product-service:$IMAGE_TAG
kubectl -n $NS set image deployment/market-service market-service=sang234/market-service:$IMAGE_TAG
kubectl -n $NS set image deployment/payment-service payment-service=sang234/payment-service:$IMAGE_TAG
kubectl -n $NS set image deployment/settlement-service settlement-service=sang234/settlement-service:$IMAGE_TAG
```

### 2-3. 롤아웃 확인
```bash
kubectl -n thock-prod rollout status deploy/api-gateway --timeout=300s
kubectl -n thock-prod rollout status deploy/member-service --timeout=300s
kubectl -n thock-prod rollout status deploy/product-service --timeout=300s
kubectl -n thock-prod rollout status deploy/market-service --timeout=300s
kubectl -n thock-prod rollout status deploy/payment-service --timeout=300s
kubectl -n thock-prod rollout status deploy/settlement-service --timeout=300s
```

### 2-4. 필수 검증
```bash
# 실행 이미지 확인 (latest 금지)
kubectl -n thock-prod get deploy api-gateway member-service product-service market-service payment-service settlement-service \
  -o=jsonpath='{range .items[*]}{.metadata.name}{" => "}{.spec.template.spec.containers[0].image}{"\n"}{end}'

# Pod 상태
kubectl -n thock-prod get pods

# 내부 헬스체크
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://member-service:8081/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://product-service:8082/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://market-service:8083/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://payment-service:8084/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://settlement-service:8085/actuator/health'
```

---

## 3. 긴급 롤백
```bash
kubectl -n thock-prod rollout undo deploy/api-gateway
kubectl -n thock-prod rollout undo deploy/member-service
kubectl -n thock-prod rollout undo deploy/product-service
kubectl -n thock-prod rollout undo deploy/market-service
kubectl -n thock-prod rollout undo deploy/payment-service
kubectl -n thock-prod rollout undo deploy/settlement-service
```

또는 특정 태그로 즉시 되돌리기:
```bash
IMAGE_TAG=<이전_정상_SHA>
# 2-2의 set image 절차 재실행
```

롤백 후 필수 확인:
```bash
kubectl -n thock-prod get pods
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 30
```

---

## 4. Kubernetes ↔ Compose 전환

### 4-1. Kubernetes -> Compose
```bash
sudo systemctl stop k3s
sudo systemctl status k3s

cd ~/deploy-docker-compose
docker compose down
docker compose up -d
docker compose ps
```

### 4-2. Compose -> Kubernetes
```bash
cd ~/deploy-docker-compose
docker compose down
docker ps

sudo systemctl start k3s
sudo systemctl status k3s
kubectl get nodes
kubectl -n thock-prod get pods
```

주의:
- 두 환경을 동시에 운영하지 않습니다.
- 전환 직후 DNS/Ingress 경로 기준으로 실제 응답 주체를 반드시 확인합니다.

---

## 5. Ingress 변경 표준 절차
- Ingress/Middleware 설정 변경 시에만 실행합니다.
- 상세 순서는 `deploy-kubernetes/ingress/INGRESS_GUIDE.md`를 따릅니다.

기본 실행:
```bash
cd ~/deploy-kubernetes/ingress
kubectl apply -f ip-whitelist-middleware.yaml
kubectl apply -f ingress.yaml
kubectl apply -f swagger-ingress.yaml
kubectl apply -f grafana-ingress.yaml
kubectl apply -f redpanda-ingress.yaml
kubectl -n thock-prod get ingress
```

---

## 6. 관리자 경로 보안 검증
- Swagger/Grafana/Redpanda는 관리자 IP만 허용해야 합니다.

허용 IP에서 기대:
- 200 또는 302

비허용 IP에서 기대:
- 403

검증 예시:
```bash
curl -Ik https://api.thock.site/swagger-ui.html
curl -Ik https://api.thock.site/grafana/
curl -Ik https://api.thock.site/redpanda/
```

---

## 7. 자주 발생하는 실수
- `kubectl apply`만 하고 `set image`를 생략함
- `latest` 이미지를 다시 사용함
- Ingress를 개별/무순서로 적용함
- ConfigMap/Secret 변경 후 재시작 검증을 생략함
- k3s/compose 동시 실행 상태로 테스트함

---

## 8. 관련 문서
- `deploy-kubernetes/ingress/INGRESS_GUIDE.md`
- `deploy-kubernetes/DEPLOYMENT_CAUTIONS.txt`
- `deploy-docker-compose/DEPLOYMENT_CAUTIONS.txt`
- 빠른 이동:
  - Ingress 적용 순서 상세: `AWS EC2/deploy-kubernetes/ingress/INGRESS_GUIDE.md`
  - 본 문서 치트시트: `AWS EC2/OPERATIONS_RUNBOOK.md`의 `9. 수동 점검 치트시트 (Top 10)`

---

## 9. 수동 점검 치트시트 (Top 10)
1. 현재 실행 모드 확인
```bash
sudo systemctl status k3s
docker ps
```

2. Kubernetes 핵심 파드 상태
```bash
kubectl -n thock-prod get pods -o wide
```

3. Kubernetes 롤아웃 상태
```bash
kubectl -n thock-prod rollout status deploy/api-gateway
```

4. Kubernetes 실행 이미지 태그 확인
```bash
kubectl -n thock-prod get deploy api-gateway member-service product-service market-service payment-service settlement-service \
  -o=jsonpath='{range .items[*]}{.metadata.name}{" => "}{.spec.template.spec.containers[0].image}{"\n"}{end}'
```

5. Kubernetes 이벤트 최근 확인
```bash
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 30
```

6. Kubernetes 서비스 간 헬스체크
```bash
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://member-service:8081/actuator/health'
```

7. Compose 컨테이너 상태
```bash
cd ~/deploy-docker-compose
docker compose ps
```

8. Compose 실행 이미지 확인
```bash
docker ps --format "table {{.Names}}\t{{.Image}}"
```

9. 관리자 경로 접근 정책 점검
```bash
curl -Ik https://api.thock.site/swagger-ui.html
curl -Ik https://api.thock.site/grafana/
curl -Ik https://api.thock.site/redpanda/
```

10. Ingress/Middleware 상태 점검
```bash
kubectl -n thock-prod get ingress
kubectl -n thock-prod get middleware swagger-admin-ip-whitelist -o yaml
```

---

## 10. 배포 직후 5분 체크리스트 (표준)
1. 롤아웃 성공 확인
```bash
kubectl -n thock-prod rollout status deploy/api-gateway --timeout=300s
kubectl -n thock-prod rollout status deploy/member-service --timeout=300s
kubectl -n thock-prod rollout status deploy/product-service --timeout=300s
kubectl -n thock-prod rollout status deploy/market-service --timeout=300s
kubectl -n thock-prod rollout status deploy/payment-service --timeout=300s
kubectl -n thock-prod rollout status deploy/settlement-service --timeout=300s
```

2. 파드 상태/재시작 횟수 확인
```bash
kubectl -n thock-prod get pods
```

3. 실행 이미지 태그 확인
```bash
kubectl -n thock-prod get deploy api-gateway member-service product-service market-service payment-service settlement-service \
  -o=jsonpath='{range .items[*]}{.metadata.name}{" => "}{.spec.template.spec.containers[0].image}{"\n"}{end}'
```

4. 내부 헬스체크 확인
```bash
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://member-service:8081/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://product-service:8082/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://market-service:8083/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://payment-service:8084/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://settlement-service:8085/actuator/health'
```

5. 외부 핵심 진입점 응답 확인
```bash
curl -Ik https://api.thock.site/swagger-ui.html
```

---

## 11. 장애 의심 시 1분 트리아지 (표준)
```bash
# 1) 파드 상태/재시작
kubectl -n thock-prod get pods -o wide

# 2) 최근 이벤트
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 40

# 3) 문제 파드 로그 (예: api-gateway)
kubectl -n thock-prod logs deploy/api-gateway --tail=200

# 4) 리소스 사용량
kubectl -n thock-prod top pod
```

---

## 12. 정상/비정상 판정 기준
- 정상
  - 모든 핵심 Pod가 `READY 1/1`, `STATUS Running`
  - 롤아웃 명령이 모두 `successfully rolled out`
  - 내부 헬스체크 응답이 모두 `{"status":"UP"...}`
  - 배포 대상 서비스 이미지 태그가 의도한 태그와 일치
- 비정상
  - `CrashLoopBackOff`, `ImagePullBackOff`, `Error`, `Pending` 지속
  - `rollout status` 타임아웃 또는 실패
  - `startupProbe/livenessProbe/readinessProbe failed`가 반복
  - 내부 헬스체크 실패(연결 거부, 타임아웃, 5xx)

---

## 13. 장애 대응 우선순위 (Sev 기준)

### Sev1 (치명)
- 기준:
  - 외부 핵심 API가 전면 장애(대부분 5xx/타임아웃)
  - api-gateway 또는 다수 핵심 서비스가 비정상
  - 결제/주문 등 핵심 플로우가 완전히 중단
- 즉시 조치:
```bash
# 1) 상태 확인
kubectl -n thock-prod get pods -o wide
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 50

# 2) 즉시 롤백(최근 변경이 원인으로 의심될 때)
kubectl -n thock-prod rollout undo deploy/api-gateway
kubectl -n thock-prod rollout undo deploy/member-service
kubectl -n thock-prod rollout undo deploy/product-service
kubectl -n thock-prod rollout undo deploy/market-service
kubectl -n thock-prod rollout undo deploy/payment-service
kubectl -n thock-prod rollout undo deploy/settlement-service

# 3) 롤백 결과 확인
kubectl -n thock-prod rollout status deploy/api-gateway --timeout=300s
kubectl -n thock-prod get pods
```
- 에스컬레이션:
  - 5분 내 정상화 실패 시 즉시 롤백 유지 + 원인 분석 전담 전환

### Sev2 (중대)
- 기준:
  - 일부 API/특정 도메인만 오류 증가
  - 사용자 영향이 있으나 우회 가능 또는 부분 기능 정상
- 즉시 조치:
```bash
# 1) 영향 범위 확인
kubectl -n thock-prod get pods
kubectl -n thock-prod logs deploy/api-gateway --tail=200
kubectl -n thock-prod logs deploy/<문제서비스> --tail=200

# 2) 문제 서비스만 롤백
kubectl -n thock-prod rollout undo deploy/<문제서비스>
kubectl -n thock-prod rollout status deploy/<문제서비스> --timeout=300s

# 3) 헬스체크 재검증
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://<문제서비스>:<port>/actuator/health'
```
- 에스컬레이션:
  - 10~15분 내 개선 없으면 Sev1 수준 대응(광범위 롤백/전면 점검)으로 상향

### Sev3 (경미)
- 기준:
  - 사용자 영향이 경미하거나 내부 운영성 이슈 중심
  - 일시적 경고/노이즈 로그/메트릭 이상치
- 즉시 조치:
```bash
# 1) 관찰
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 30
kubectl -n thock-prod top pod

# 2) 로그 샘플 확인
kubectl -n thock-prod logs deploy/<대상서비스> --tail=200
```
- 에스컬레이션:
  - 재현되거나 빈도 증가 시 Sev2로 상향, 운영 TODO/개선 과제로 등록

---

## 14. 롤백 vs 관찰 판단 기준
- 롤백 우선:
  - 배포 직후 즉시 장애 발생
  - 동일 시점에 다수 서비스 Probe 실패/기동 실패
  - 이미지 태그 변경 직후 오류율 급증
- 관찰 우선:
  - 단발성 타임아웃/일시 스파이크
  - 자동 복구 후 재발 없음
  - 사용자 영향이 없고 내부 지표만 일시 변동

---

## 15. 배포 후 자동 검증 스모크 테스트 (문서 기준)

실행 원칙:
- 배포 직후 5분 이내 실행
- 동일한 시나리오를 반복해서 비교 가능하게 유지
- 실패 시 즉시 `11. 장애 의심 시 1분 트리아지`로 전환

### 15-1. 공통 변수
```bash
BASE_URL="https://api.thock.site"
ACCESS_TOKEN="<유효한_토큰>"
ADMIN_IP_EXPECTED_CODE="200_or_302"
```

### 15-2. 핵심 스모크 시나리오
1. API Gateway 헬스(외부)
```bash
curl -s -o /dev/null -w "%{http_code}\n" "$BASE_URL/actuator/health"
```
기대:
- 보안 정책상 외부 차단이면 `403` 또는 `404`
- 외부 공개 정책이면 `200`

2. 내 정보 조회(인증 API)
```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$BASE_URL/api/v1/members/me"
```
기대:
- `200` (유효 토큰)
- 토큰 불량/만료 시 `401`

3. Swagger 접근 정책 확인
```bash
curl -Ik "$BASE_URL/swagger-ui.html"
```
기대:
- 관리자 허용 IP: `200` 또는 `302`
- 비허용 IP: `403`

4. Grafana 접근 정책 확인
```bash
curl -Ik "$BASE_URL/grafana/"
```
기대:
- 관리자 허용 IP: `200` 또는 `302`
- 비허용 IP: `403`

5. Redpanda Console 접근 정책 확인
```bash
curl -Ik "$BASE_URL/redpanda/"
```
기대:
- 관리자 허용 IP: `200` 또는 `302`
- 비허용 IP: `403`

6. 내부 서비스 헬스(클러스터 내부)
```bash
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://member-service:8081/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://product-service:8082/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://market-service:8083/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://payment-service:8084/actuator/health'
kubectl -n thock-prod exec deploy/api-gateway -- sh -lc 'wget -qO- http://settlement-service:8085/actuator/health'
```
기대:
- 모든 응답 `status=UP`

### 15-3. 실패 시 즉시 확인 명령어
```bash
kubectl -n thock-prod get pods -o wide
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 40
kubectl -n thock-prod logs deploy/api-gateway --tail=200
kubectl -n thock-prod top pod
```

### 15-4. 판정
- 통과:
  - 핵심 API 인증 호출 성공
  - 관리자 경로 접근 정책 기대값 충족
  - 내부 헬스체크 전부 `UP`
- 실패:
  - 인증 API 연속 실패(401/403/5xx 비정상)
  - 관리자 경로 정책 불일치(허용 IP인데 403 또는 비허용 IP인데 200/302)
  - 내부 헬스체크 실패 1개 이상

---

## 16. 헬스체크/프로브 정책 통일 (표준)

### 16-1. 기본 표준값 (운영 기준)
- `startupProbe`
  - `httpGet.path`: `/actuator/health/liveness`
  - `periodSeconds`: `10`
  - `timeoutSeconds`: `5`
  - `failureThreshold`: `30`
  - `initialDelaySeconds`: `30`
- `livenessProbe`
  - `httpGet.path`: `/actuator/health/liveness`
  - `periodSeconds`: `10`
  - `timeoutSeconds`: `5`
  - `failureThreshold`: `3`
- `readinessProbe`
  - `httpGet.path`: `/actuator/health/readiness`
  - `periodSeconds`: `5`
  - `timeoutSeconds`: `3`
  - `failureThreshold`: `3`

### 16-2. 서비스별 예외 허용 기준
- 예외를 허용하는 경우:
  - 초기 로딩이 긴 서비스(JPA 초기화, 외부 의존성 초기 연결이 긴 경우)
  - 정기 배치/워밍업으로 시작 직후 CPU 스파이크가 큰 서비스
- 예외 적용 원칙:
  - 우선 `startupProbe`만 완화하고 `liveness/readiness`는 기본값 유지
  - 완화는 최소 단위로 증가(`initialDelaySeconds` +10~20, `failureThreshold` 소폭 증가)
  - 예외 사유와 적용값을 문서/PR에 명시

### 16-3. 변경 시 검증 절차
```bash
# 1) 배포 반영
kubectl -n thock-prod apply -f ~/deploy-kubernetes/services/
kubectl -n thock-prod rollout restart deploy/api-gateway deploy/member-service deploy/product-service deploy/market-service deploy/payment-service deploy/settlement-service

# 2) 롤아웃 확인
kubectl -n thock-prod rollout status deploy/api-gateway --timeout=300s
kubectl -n thock-prod rollout status deploy/member-service --timeout=300s
kubectl -n thock-prod rollout status deploy/product-service --timeout=300s
kubectl -n thock-prod rollout status deploy/market-service --timeout=300s
kubectl -n thock-prod rollout status deploy/payment-service --timeout=300s
kubectl -n thock-prod rollout status deploy/settlement-service --timeout=300s

# 3) 실패 이벤트 확인
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 50

# 4) 리소스 여유 확인
kubectl -n thock-prod top pod
```

판정 기준:
- 통과:
  - `Startup probe failed` 경고가 초기 1~2회 이내로 수렴하고 재시작 없이 안정화
  - 모든 핵심 Pod `READY 1/1`
- 실패:
  - probe 실패가 반복되어 재시작/롤링 루프 발생
  - rollout timeout 또는 readiness 미충족 지속

---

## 17. 운영 로그 보존/정리 정책 (표준)

### 17-1. 보존 기간 기준
- 애플리케이션 파일 로그(`/app/logs/*.log`)
  - 기본 보존: `14일`
  - 장애 분석 대상 기간: `최근 3일`은 우선 보존
- 인프라/플랫폼 로그(Kubernetes 이벤트/Pod 로그)
  - 운영 점검: `최근 24시간`
  - 장애 분석: `장애 시각 전후 2시간` 우선 수집

### 17-2. 디스크 임계치 기준과 정리
- 임계치:
  - `70%` 도달: 정리 준비(대용량 로그 파일 확인)
  - `80%` 도달: 즉시 정리 실행
  - `90%` 이상: 장애 대응 우선순위로 승격(Sev2 이상)

확인 명령어:
```bash
df -h
du -sh /app/logs 2>/dev/null || true
du -ah /app/logs 2>/dev/null | sort -rh | head -n 30
```

정리 원칙:
- 당일/최근 장애 분석 로그는 삭제하지 않음
- 오래된 압축 로그부터 정리
- 정리 후 필수 재확인:
```bash
df -h
kubectl -n thock-prod get pods
```

### 17-3. 장애 시 로그 수집 우선순위
1. `api-gateway`
- 외부 요청 진입점이며 에러 전파를 가장 빨리 확인 가능
```bash
kubectl -n thock-prod logs deploy/api-gateway --tail=300
```

2. 장애 증상 서비스(예: `payment-service`, `market-service`)
```bash
kubectl -n thock-prod logs deploy/<문제서비스> --tail=300
```

3. 공통 인프라 상태(Kubernetes 이벤트)
```bash
kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 50
```

4. 리소스 압박 확인(CPU/메모리)
```bash
kubectl -n thock-prod top pod
```

### 17-4. 운영 규칙
- 배포 직후에는 로그 레벨/로그량 급증 여부를 10분 모니터링
- 불필요한 DEBUG SQL 로그는 운영 환경에서 비활성 유지
- 로그 정책 변경 시 Compose/Kubernetes 모두 동일 기준으로 반영

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
| v1.0.0 | 2026-03-05 | ops-admin | 운영 표준 절차 문서 통합 및 섹션 표준화 |
| v1.0.1 | 2026-03-05 | ops-admin | 신규 정책 문서(모니터링/DB/보안) 공통 링크 반영 |
