# Kubernetes 빠른 시작 가이드

이 가이드는 처음 Kubernetes를 사용하는 사람들을 위한 간단한 배포 가이드입니다.

## 1. 사전 준비 (5분)

### kubectl 설치

```bash
# macOS
brew install kubectl

# 설치 확인
kubectl version --client
```

### Kubernetes 클러스터 연결

이미 클러스터가 있다면:

```bash
# AWS EKS 예시
aws eks update-kubeconfig --name your-cluster-name --region ap-northeast-2

# 연결 확인
kubectl get nodes
```

로컬 테스트용 클러스터가 필요하다면:

```bash
# Minikube 설치 및 시작
brew install minikube
minikube start --cpus 4 --memory 8192
```

## 2. Secret 파일 준비 (2분)

```bash
# 1. Secret 템플릿 복사
cp base/secrets.yaml.template base/secrets.yaml

# 2. 실제 비밀번호 입력 (vim 또는 원하는 에디터 사용)
vim base/secrets.yaml

# 최소한 다음 항목들을 변경하세요:
# - JWT_SECRET_KEY
# - MYSQL_ROOT_PASSWORD
# - *_DB_PASSWORD (모든 서비스 DB 비밀번호)
# - GRAFANA_ADMIN_PASSWORD
```

**중요**: 이 파일은 git에 커밋하지 마세요! (이미 .gitignore에 추가되어 있습니다)

## 3. 배포 실행 (10-15분)

### 자동 배포 (권장)

```bash
# 실행 권한 부여 (한 번만)
chmod +x deploy.sh

# 전체 배포
./deploy.sh
```

배포가 완료되면 모든 서비스가 자동으로 시작됩니다!

### 수동 배포

자동 스크립트 대신 단계별로 배포하려면:

```bash
# 1. 네임스페이스
kubectl apply -f base/namespace.yaml

# 2. ConfigMap & Secret
kubectl apply -f base/configmap.yaml
kubectl apply -f database/mysql-configmap.yaml
kubectl apply -f base/secrets.yaml

# 3. 데이터베이스
kubectl apply -f database/mysql-statefulset.yaml
kubectl wait --for=condition=ready pod -l app=mysql -n thock-prod --timeout=300s

# 4. 메시징
kubectl apply -f messaging/redpanda-statefulset.yaml
kubectl apply -f messaging/redpanda-console.yaml

# 5. 애플리케이션 서비스
kubectl apply -f services/
kubectl rollout status deployment/api-gateway -n thock-prod

# 6. 모니터링
kubectl apply -f monitoring/

# 7. Ingress (선택)
kubectl apply -f ingress/

# 8. HPA (선택)
kubectl apply -f base/hpa.yaml
```

## 4. 상태 확인

```bash
# 간단한 상태 확인
kubectl get pods -n thock-prod

# 전체 상태 확인 (스크립트)
./status.sh
```

모든 Pod가 `Running` 상태가 될 때까지 기다리세요 (약 5-10분).

## 5. 애플리케이션 접근

### API Gateway 테스트

**Port-Forward로 로컬 접속:**

```bash
kubectl port-forward svc/api-gateway 8080:8080 -n thock-prod
```

브라우저에서 `http://localhost:8080` 접속

### Grafana 모니터링

```bash
kubectl port-forward svc/grafana 3000:3000 -n thock-prod
```

브라우저에서 `http://localhost:3000` 접속
- 사용자명: `admin`
- 비밀번호: secrets.yaml에서 설정한 값

### Redpanda Console

```bash
kubectl port-forward svc/redpanda-console 8090:8090 -n thock-prod
```

브라우저에서 `http://localhost:8090` 접속

## 6. 문제 해결

### Pod가 시작되지 않을 때

```bash
# Pod 상태 확인
kubectl get pods -n thock-prod

# 문제가 있는 Pod의 상세 정보
kubectl describe pod <pod-name> -n thock-prod

# 로그 확인
kubectl logs <pod-name> -n thock-prod
```

### 흔한 문제들

**1. ImagePullBackOff**
- 원인: Docker 이미지를 가져올 수 없음
- 해결: 이미지 이름과 태그 확인

**2. CrashLoopBackOff**
- 원인: 애플리케이션이 계속 재시작됨
- 해결: `kubectl logs <pod-name> -n thock-prod --previous` 로 로그 확인

**3. Pending (PVC)**
- 원인: PersistentVolume을 생성할 수 없음
- 해결: StorageClass 확인 (`kubectl get storageclass`)

**4. Secret not found**
- 원인: Secret이 생성되지 않음
- 해결: `kubectl apply -f base/secrets.yaml`

## 7. 업데이트 배포

새 버전을 배포하려면:

```bash
# 새 이미지로 업데이트 (예: member-service)
kubectl set image deployment/member-service \
  member-service=sang234/member-service:new-tag \
  -n thock-prod

# 롤아웃 상태 확인
kubectl rollout status deployment/member-service -n thock-prod

# 롤백이 필요하면
kubectl rollout undo deployment/member-service -n thock-prod
```

## 8. 정리 (삭제)

### 개별 리소스만 삭제

```bash
./cleanup.sh
```

### 모든 것 삭제 (네임스페이스 포함)

```bash
./cleanup.sh --all
```

## 9. 유용한 명령어 모음

```bash
# 모든 리소스 조회
kubectl get all -n thock-prod

# Pod 로그 실시간 확인
kubectl logs -f <pod-name> -n thock-prod

# Pod 내부 접속
kubectl exec -it <pod-name> -n thock-prod -- /bin/bash

# 리소스 사용량
kubectl top pods -n thock-prod

# 서비스 상세 정보
kubectl describe svc <service-name> -n thock-prod
```

## 10. 다음 단계

배포가 성공했다면:

1. **Ingress 설정**: 외부 도메인으로 접근 가능하게 만들기
2. **SSL 인증서**: Let's Encrypt로 HTTPS 활성화
3. **모니터링 대시보드**: Grafana 대시보드 커스터마이징
4. **자동 스케일링**: HPA 설정으로 트래픽에 따라 자동 확장
5. **CI/CD**: GitHub Actions로 자동 배포 파이프라인 구성

자세한 내용은 [README.md](README.md)를 참고하세요!

## 도움이 필요하신가요?

- 전체 문서: [README.md](README.md)
- 상태 확인: `./status.sh`
- 로그 확인: `kubectl logs -f <pod-name> -n thock-prod`
- 팀에 문의하기