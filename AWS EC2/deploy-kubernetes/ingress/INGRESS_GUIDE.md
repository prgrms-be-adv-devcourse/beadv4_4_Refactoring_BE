# Ingress 적용 순서 가이드

## 관련 문서 바로가기
- 통합 운영 절차/점검 명령어: `AWS EC2/OPERATIONS_RUNBOOK.md`
- 런북 치트시트 위치: `AWS EC2/OPERATIONS_RUNBOOK.md`의 `9. 수동 점검 치트시트 (Top 10)`

## 적용이 필요한 경우
- Ingress/Middleware 설정을 수정한 경우에만 실행합니다.
- 단순 Pod 재시작, EC2 재부팅, k3s 재시작만 한 경우에는 불필요합니다.

## 적용 순서 (중요)
1. Middleware
2. Main Ingress (`ingress.yaml`)
3. Swagger Ingress (`swagger-ingress.yaml`)
4. Grafana Ingress (`grafana-ingress.yaml`)
5. Redpanda Ingress (`redpanda-ingress.yaml`)

-> 사실 미들웨어가 먼저 적용되는 것이 중요한 것이지 나머지 순서는 크게 상관 X

## 실행 명령어
```bash
cd ~/deploy-kubernetes/ingress

kubectl apply -f ip-whitelist-middleware.yaml
kubectl apply -f ingress.yaml
kubectl apply -f swagger-ingress.yaml
kubectl apply -f grafana-ingress.yaml
kubectl apply -f redpanda-ingress.yaml
```

## 확인 명령어
```bash
kubectl -n thock-prod get middleware swagger-admin-ip-whitelist -o yaml
kubectl -n thock-prod get ingress
```

## 운영 규칙
- `ingress.yaml`에는 메인 API 경로(`/`)만 유지합니다.
- `/swagger`, `/grafana`, `/redpanda` 계열은 각 전용 ingress에서만 관리합니다.
- 적용 순서를 섞어서 실행하지 않습니다.
