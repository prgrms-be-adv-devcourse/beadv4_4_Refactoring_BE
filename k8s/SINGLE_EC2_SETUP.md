# 단일 EC2에서 Docker Compose → K3s 전환 가이드

**학습용** 단일 EC2 인스턴스에서 Docker Compose를 K3s(경량 Kubernetes)로 전환하는 완벽 가이드입니다.

---

## 전제 조건

- AWS EC2 인스턴스 (Ubuntu 22.04)
- 인스턴스 타입: **t3.medium** (4GB RAM) 이상 권장
- 기존 Docker Compose로 실행 중인 서비스

---

## 빠른 시작 (10분)

### 1단계: EC2에 SSH 접속

```bash
ssh ubuntu@your-ec2-public-ip
```

### 2단계: 기존 Docker Compose 중단 (선택사항)

```bash
cd /path/to/your/docker-compose
docker compose down

# 백업
cp docker-compose.yml docker-compose.yml.backup
cp .env .env.backup
```

### 3단계: K3s 설치 (1분)

```bash
# K3s 설치
curl -sfL https://get.k3s.io | sh -

# kubectl 설정 (sudo 없이 사용)
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config

# 확인
kubectl get nodes
```

**출력 예시:**
```
NAME        STATUS   ROLES                  AGE   VERSION
ip-xxx      Ready    control-plane,master   30s   v1.28.5+k3s1
```

### 4단계: 프로젝트 파일 업로드

**로컬 PC에서:**

```bash
# k8s 디렉토리를 EC2로 복사
cd /path/to/your/project
scp -r k8s ubuntu@your-ec2-ip:~/
```

**또는 Git 사용:**

```bash
# EC2에서
git clone https://your-repo-url.git
cd your-project
```

### 5단계: Secret 파일 생성

```bash
cd ~/k8s

# Secret 템플릿 복사
cp base/secrets.yaml.template base/secrets.yaml

# 비밀번호 입력
nano base/secrets.yaml
# 또는
vim base/secrets.yaml
```

**최소한 다음 항목 변경:**
```yaml
JWT_SECRET_KEY: "your-actual-jwt-secret-key"
MYSQL_ROOT_PASSWORD: "your-mysql-root-password"
MEMBER_DB_PASSWORD: "your-member-db-password"
PRODUCT_DB_PASSWORD: "your-product-db-password"
MARKET_DB_PASSWORD: "your-market-db-password"
PAYMENT_DB_PASSWORD: "your-payment-db-password"
SETTLEMENT_DB_PASSWORD: "your-settlement-db-password"
GRAFANA_ADMIN_PASSWORD: "your-grafana-admin-password"
```

### 6단계: K3s용 배포 스크립트 실행

```bash
# 실행 권한 부여
chmod +x deploy-k3s.sh

# 배포 (전체)
./deploy-k3s.sh

# 또는 모니터링 제외 (리소스 절약)
./deploy-k3s.sh --skip-monitoring
```

### 7단계: 배포 확인 (5-10분 소요)

```bash
# 상태 확인
./status.sh

# Pod 확인
kubectl get pods -n thock-prod

# 모든 Pod가 Running 상태가 될 때까지 대기
```

### 8단계: 애플리케이션 접근

#### 옵션 A: Port-Forward (로컬 PC에서 테스트)

**새 터미널을 열고:**

```bash
# EC2에서 실행 (백그라운드)
kubectl port-forward svc/api-gateway 8080:8080 -n thock-prod &

# 이제 EC2의 8080 포트로 접속 가능
curl http://localhost:8080
```

**로컬 PC에서 접근하려면 SSH 터널링:**

```bash
# 로컬 PC에서 실행
ssh -L 8080:localhost:8080 ubuntu@your-ec2-ip

# 이제 로컬 브라우저에서
# http://localhost:8080 접속
```

#### 옵션 B: NodePort (간단한 외부 접근)

EC2에서 `services/api-gateway.yaml` 수정:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: thock-prod
spec:
  type: NodePort  # 변경
  selector:
    app: api-gateway
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30080  # 추가
```

```bash
# 적용
kubectl apply -f services/api-gateway.yaml

# EC2 보안그룹에서 30080 포트 열기 (AWS 콘솔)
# 인바운드 규칙 추가: TCP 30080, 소스 0.0.0.0/0

# 브라우저에서 접속
http://your-ec2-public-ip:30080
```

#### 옵션 C: Ingress + 도메인 (프로덕션 스타일)

**1. EC2 보안그룹에서 80, 443 포트 열기**

AWS 콘솔:
- 인바운드 규칙 추가: TCP 80, 0.0.0.0/0
- 인바운드 규칙 추가: TCP 443, 0.0.0.0/0

**2. 도메인 설정 (Route53)**

```
A 레코드 추가:
api.thock.site → your-ec2-public-ip
```

**3. Cert-Manager 설치 (SSL 자동 발급)**

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# 준비 확인 (2-3분)
kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=cert-manager -n cert-manager --timeout=300s
```

**4. Ingress 배포**

```bash
kubectl apply -f ingress/cert-issuer.yaml
kubectl apply -f ingress/ingress.yaml

# SSL 인증서 발급 확인 (5-10분)
kubectl get certificate -n thock-prod
```

**5. 접속**

```
https://api.thock.site
```

---

## 자주 묻는 질문 (FAQ)

### Q1: Docker Compose와 K3s를 동시에 실행할 수 있나요?

**A**: 아니요, 포트 충돌이 발생합니다. Docker Compose를 중단하고 K3s를 실행하세요.

### Q2: EC2 사양이 부족한 것 같아요.

**A**: 다음을 시도하세요:
- 모니터링 스택 제외: `./deploy-k3s.sh --skip-monitoring`
- EC2 타입 업그레이드: t3.small → t3.medium → t3.large
- 일부 서비스만 배포

### Q3: Pod가 계속 Pending 상태입니다.

**A**: 리소스 부족입니다. 다음을 확인하세요:

```bash
# 리소스 사용량
kubectl top nodes
kubectl top pods -n thock-prod

# Pod 상세 확인
kubectl describe pod <pod-name> -n thock-prod

# 이벤트 확인
kubectl get events -n thock-prod --sort-by='.lastTimestamp'
```

해결 방법:
- 불필요한 서비스 중단
- EC2 메모리 증가
- 리소스 요청 감소

### Q4: 외부에서 접근이 안 됩니다.

**A**: 체크리스트:
- [ ] EC2 보안그룹에서 해당 포트 열려 있는지 확인
- [ ] Kubernetes Service 타입 확인 (`kubectl get svc -n thock-prod`)
- [ ] Ingress 상태 확인 (`kubectl get ingress -n thock-prod`)
- [ ] EC2 Public IP 확인

### Q5: Docker Compose로 다시 돌아가고 싶습니다.

**A**:

```bash
# K3s 제거
/usr/local/bin/k3s-uninstall.sh

# Docker Compose 복구
cd /path/to/your/project
docker compose up -d
```

---

## 리소스 사용량 모니터링

### EC2 리소스 확인

```bash
# 메모리 사용량
free -h

# CPU 사용량
top

# 디스크 사용량
df -h
```

### Kubernetes 리소스 확인

```bash
# 노드 리소스
kubectl top nodes

# Pod 리소스
kubectl top pods -n thock-prod

# 네임스페이스별 리소스
kubectl top pods -n kube-system
```

---

## 트러블슈팅

### 1. K3s 서비스가 시작되지 않음

```bash
# K3s 상태 확인
sudo systemctl status k3s

# K3s 로그 확인
sudo journalctl -u k3s -f

# K3s 재시작
sudo systemctl restart k3s
```

### 2. Pod가 ImagePullBackOff

```bash
kubectl describe pod <pod-name> -n thock-prod

# 원인: Docker 이미지를 가져올 수 없음
# 해결:
# 1. 이미지 이름/태그 확인
# 2. 이미지가 Public인지 확인
# 3. Private이면 ImagePullSecret 추가
```

### 3. PVC가 Pending

```bash
kubectl get pvc -n thock-prod
kubectl describe pvc <pvc-name> -n thock-prod

# K3s StorageClass 확인
kubectl get storageclass

# local-path가 있어야 함
# 없으면 deploy-k3s.sh를 다시 실행
```

### 4. OOMKilled (메모리 부족)

```bash
# Pod 상태 확인
kubectl get pods -n thock-prod

# 해결:
# 1. 리소스 제한 조정
# 2. 불필요한 서비스 중단
# 3. EC2 메모리 증가
```

---

## 성능 최적화

### 단일 EC2용 리소스 조정

**자동 조정 스크립트:**

```bash
cd ~/k8s

# 리소스 요청 감소
find services/ -name "*.yaml" -exec sed -i 's/memory: "512Mi"/memory: "256Mi"/g' {} \;
find services/ -name "*.yaml" -exec sed -i 's/cpu: "250m"/cpu: "100m"/g' {} \;

# 리소스 제한 감소
find services/ -name "*.yaml" -exec sed -i 's/memory: "1Gi"/memory: "512Mi"/g' {} \;
find services/ -name "*.yaml" -exec sed -i 's/cpu: "1000m"/cpu: "500m"/g' {} \;

# HPA 조정 (최소 1개, 최대 2개 Pod)
sed -i 's/minReplicas: 2/minReplicas: 1/g' base/hpa.yaml
sed -i 's/maxReplicas: 10/maxReplicas: 2/g' base/hpa.yaml

# 재배포
kubectl apply -f services/
kubectl apply -f base/hpa.yaml
```

### 선택적 서비스 배포

전체가 아닌 일부만 배포:

```bash
# 핵심 인프라만
kubectl apply -f base/namespace.yaml
kubectl apply -f base/configmap.yaml
kubectl apply -f base/secrets.yaml
kubectl apply -f database/

# 필요한 서비스만
kubectl apply -f services/api-gateway.yaml
kubectl apply -f services/member-service.yaml
```

---

## 유용한 명령어 모음

### K3s 관리

```bash
# K3s 상태
sudo systemctl status k3s

# K3s 재시작
sudo systemctl restart k3s

# K3s 로그
sudo journalctl -u k3s -f
```

### Kubernetes 기본

```bash
# 모든 리소스 조회
kubectl get all -n thock-prod

# Pod 로그
kubectl logs -f <pod-name> -n thock-prod

# Pod 내부 접속
kubectl exec -it <pod-name> -n thock-prod -- /bin/bash

# 서비스 상세 정보
kubectl describe svc <service-name> -n thock-prod

# 이벤트 확인
kubectl get events -n thock-prod --sort-by='.lastTimestamp'
```

### Port-Forward

```bash
# API Gateway
kubectl port-forward svc/api-gateway 8080:8080 -n thock-prod

# Grafana
kubectl port-forward svc/grafana 3000:3000 -n thock-prod

# MySQL
kubectl port-forward svc/mysql 3306:3306 -n thock-prod

# Redpanda Console
kubectl port-forward svc/redpanda-console 8090:8090 -n thock-prod
```

---

## 학습 체크리스트

K3s를 통해 다음을 학습할 수 있습니다:

- [ ] Kubernetes 기본 개념 (Pod, Service, Deployment)
- [ ] ConfigMap과 Secret 관리
- [ ] PersistentVolume과 StorageClass
- [ ] Ingress와 외부 접근
- [ ] Liveness/Readiness Probe
- [ ] HPA (자동 스케일링)
- [ ] 롤링 업데이트
- [ ] kubectl 명령어
- [ ] 로그 확인 및 디버깅
- [ ] 리소스 관리

---

## 다음 단계

학습이 끝나면:

1. **EKS로 이전**: 프로덕션 레벨 경험
2. **Helm 학습**: 패키지 관리 도구
3. **GitOps (ArgoCD)**: 자동화된 배포
4. **Service Mesh (Istio)**: 고급 트래픽 관리
5. **Monitoring (Prometheus/Grafana)**: 깊이 있는 모니터링

---

## 참고 자료

- **K3s 공식 문서**: https://docs.k3s.io/
- **Kubernetes 공식 문서 (한글)**: https://kubernetes.io/ko/docs/
- **kubectl Cheat Sheet**: https://kubernetes.io/docs/reference/kubectl/cheatsheet/
- **전체 가이드**: [README.md](README.md)
- **K3s 상세 가이드**: [K3S_GUIDE.md](K3S_GUIDE.md)