# Kubernetes (K3s) Deployment Guide

이 디렉토리는 Thock 애플리케이션을 K3s(경량 Kubernetes)로 배포하기 위한 모든 매니페스트 파일을 포함합니다.

**기본 설정**: 단일 EC2에서 K3s 사용 (학습 및 개발 환경)
**확장 가능**: AWS EKS 등 프로덕션 환경으로 전환 가능

## 목차

1. [사전 요구사항](#사전-요구사항)
2. [디렉토리 구조](#디렉토리-구조)
3. [빠른 시작](#빠른-시작)
4. [단계별 배포](#단계별-배포)
5. [설정 커스터마이징](#설정-커스터마이징)
6. [모니터링](#모니터링)
7. [트러블슈팅](#트러블슈팅)
8. [유용한 명령어](#유용한-명령어)

---

## 사전 요구사항

### 필수 도구

**단일 EC2 (K3s) 사용 시:**
- EC2 인스턴스 (Ubuntu 22.04, t3.medium 이상)
- K3s 설치 (자동으로 kubectl 포함)

**프로덕션 (EKS) 사용 시:**
- **kubectl**: Kubernetes CLI 도구
  ```bash
  # macOS
  brew install kubectl

  # Linux
  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
  chmod +x kubectl
  sudo mv kubectl /usr/local/bin/
  ```

- **eksctl**: EKS 클러스터 관리 도구
  ```bash
  brew install eksctl  # macOS
  ```

### 선택사항 (권장)

- **Helm**: Kubernetes 패키지 매니저
  ```bash
  brew install helm  # macOS
  ```

- **cert-manager**: SSL 인증서 자동 관리
  ```bash
  kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
  ```

- **metrics-server**: HPA(Auto Scaling)를 위한 메트릭 수집
  ```bash
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
  ```

- **Nginx Ingress Controller**: 외부 트래픽 라우팅
  ```bash
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/aws/deploy.yaml
  ```

---

## 디렉토리 구조

```
k3s/
├── base/                    # 기본 설정
│   ├── namespace.yaml       # 네임스페이스
│   ├── configmap.yaml       # 공통 환경변수
│   ├── secrets.yaml.template # 비밀정보 템플릿
│   └── hpa.yaml             # 자동 스케일링
├── database/                # 데이터베이스
│   ├── mysql-configmap.yaml # MySQL 초기화 스크립트
│   └── mysql-statefulset.yaml # MySQL StatefulSet
├── services/                # 애플리케이션 서비스
│   ├── api-gateway.yaml
│   ├── member-service.yaml
│   ├── product-service.yaml
│   ├── market-service.yaml
│   ├── payment-service.yaml
│   └── settlement-service.yaml
├── messaging/               # 메시징 시스템
│   ├── redpanda-statefulset.yaml
│   └── redpanda-console.yaml
├── monitoring/              # 모니터링 스택
│   ├── prometheus-config.yaml
│   ├── prometheus.yaml
│   ├── loki-config.yaml
│   ├── loki.yaml
│   ├── promtail-config.yaml
│   ├── promtail.yaml
│   └── grafana.yaml
├── ingress/                 # 외부 접근 설정
│   ├── ingress.yaml
│   └── cert-issuer.yaml
├── deploy.sh                # 자동 배포 스크립트
└── README.md                # 이 문서
```

---

## 빠른 시작 (K3s - 권장)

**완전 초보자는 [SINGLE_EC2_SETUP.md](SINGLE_EC2_SETUP.md)를 먼저 읽으세요!**

### 1. EC2에 K3s 설치 (1분)

```bash
# EC2에 SSH 접속
ssh ubuntu@your-ec2-ip

# K3s 설치
curl -sfL https://get.k3s.io | sh -

# kubectl 설정
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config

# 확인
kubectl get nodes
```

### 2. 프로젝트 파일 업로드

```bash
# 로컬 PC에서
scp -r k3s ubuntu@your-ec2-ip:~/

# EC2에서
cd ~/k3s
```

### 3. Secret 파일 생성

```bash
cp base/secrets.yaml.template base/secrets.yaml
vim base/secrets.yaml  # 비밀번호 입력
```

### 4. 배포 실행

```bash
# 전체 배포
./deploy.sh

# 또는 모니터링 제외 (리소스 절약)
./deploy.sh --skip-monitoring
```

### 5. 접근

```bash
# Port-Forward로 테스트
kubectl port-forward svc/api-gateway 8080:8080 -n thock-prod
# http://localhost:8080
```

---

## 단계별 배포

자동 스크립트 대신 수동으로 배포하려면:

### Step 1: 네임스페이스 생성

```bash
kubectl apply -f base/namespace.yaml
```

### Step 2: ConfigMap 생성

```bash
kubectl apply -f base/configmap.yaml
kubectl apply -f database/mysql-configmap.yaml
kubectl apply -f monitoring/prometheus-config.yaml
kubectl apply -f monitoring/loki-config.yaml
kubectl apply -f monitoring/promtail-config.yaml
```

### Step 3: Secret 생성

```bash
kubectl apply -f base/secrets.yaml
```

### Step 4: 데이터베이스 배포

```bash
kubectl apply -f database/mysql-statefulset.yaml

# MySQL이 준비될 때까지 대기
kubectl wait --for=condition=ready pod -l app=mysql -n thock-prod --timeout=300s
```

### Step 5: 메시징 시스템 배포

```bash
kubectl apply -f messaging/redpanda-statefulset.yaml
kubectl apply -f messaging/redpanda-console.yaml

# Redpanda가 준비될 때까지 대기
kubectl wait --for=condition=ready pod -l app=redpanda -n thock-prod --timeout=300s
```

### Step 6: 애플리케이션 서비스 배포

```bash
# 순서대로 배포 (의존성 고려)
kubectl apply -f services/member-service.yaml
kubectl apply -f services/product-service.yaml
kubectl apply -f services/payment-service.yaml
kubectl apply -f services/settlement-service.yaml
kubectl apply -f services/market-service.yaml
kubectl apply -f services/api-gateway.yaml

# 모든 서비스가 준비될 때까지 대기
kubectl rollout status deployment/api-gateway -n thock-prod
```

### Step 7: 모니터링 스택 배포

```bash
kubectl apply -f monitoring/prometheus.yaml
kubectl apply -f monitoring/loki.yaml
kubectl apply -f monitoring/promtail.yaml
kubectl apply -f monitoring/grafana.yaml
```

### Step 8: Ingress 배포

```bash
# Cert-Manager ClusterIssuer 생성
kubectl apply -f ingress/cert-issuer.yaml

# Ingress 생성
kubectl apply -f ingress/ingress.yaml
```

### Step 9: HPA 배포

```bash
kubectl apply -f base/hpa.yaml
```

---

## 설정 커스터마이징

### 리소스 제한 조정

각 서비스의 YAML 파일에서 `resources` 섹션을 수정:

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### 레플리카 수 변경

```yaml
spec:
  replicas: 3  # 원하는 Pod 수
```

### HPA 임계값 조정

`base/hpa.yaml`에서 CPU/메모리 임계값 수정:

```yaml
metrics:
- type: Resource
  resource:
    name: cpu
    target:
      type: Utilization
      averageUtilization: 70  # CPU 70% 이상 시 스케일 아웃
```

### 스토리지 클래스 변경

AWS EKS를 사용하는 경우, 각 PVC의 `storageClassName`을 주석 해제:

```yaml
# AWS EKS의 경우
storageClassName: gp3
```

---

## 모니터링

### Grafana 접속

**방법 1: Port-Forward (로컬 테스트)**

```bash
kubectl port-forward svc/grafana 3000:3000 -n thock-prod
```

브라우저에서 `http://localhost:3000` 접속

**방법 2: Ingress를 통한 접속 (프로덕션)**

`https://api.thock.site/grafana`

### Prometheus 접속

```bash
kubectl port-forward svc/prometheus 9090:9090 -n thock-prod
```

브라우저에서 `http://localhost:9090` 접속

### Redpanda Console 접속

```bash
kubectl port-forward svc/redpanda-console 8090:8090 -n thock-prod
```

브라우저에서 `http://localhost:8090` 접속

---

## 트러블슈팅

### Pod가 시작되지 않는 경우

```bash
# Pod 상태 확인
kubectl get pods -n thock-prod

# Pod 상세 정보 확인
kubectl describe pod <pod-name> -n thock-prod

# Pod 로그 확인
kubectl logs -f <pod-name> -n thock-prod

# 이전 컨테이너 로그 확인 (CrashLoopBackOff 시)
kubectl logs <pod-name> -n thock-prod --previous
```

### Secret이 없는 경우

```bash
# Secret 존재 확인
kubectl get secret app-secrets -n thock-prod

# Secret 생성
kubectl apply -f base/secrets.yaml
```

### PersistentVolume 문제

```bash
# PVC 상태 확인
kubectl get pvc -n thock-prod

# PVC 상세 정보
kubectl describe pvc <pvc-name> -n thock-prod

# StorageClass 확인
kubectl get storageclass
```

### Ingress가 작동하지 않는 경우

```bash
# Ingress 상태 확인
kubectl get ingress -n thock-prod

# Ingress Controller 확인
kubectl get pods -n ingress-nginx

# Cert-Manager 인증서 확인
kubectl get certificate -n thock-prod
kubectl describe certificate thock-tls -n thock-prod
```

### MySQL 연결 실패

```bash
# MySQL Pod 접속
kubectl exec -it mysql-0 -n thock-prod -- mysql -u root -p

# 데이터베이스 확인
SHOW DATABASES;

# 사용자 권한 확인
SELECT user, host FROM mysql.user;
```

---

## 유용한 명령어

### 전체 리소스 조회

```bash
kubectl get all -n thock-prod
```

### Pod 로그 실시간 확인

```bash
kubectl logs -f <pod-name> -n thock-prod
```

### Pod 내부 접속

```bash
kubectl exec -it <pod-name> -n thock-prod -- /bin/bash
```

### 리소스 사용량 확인

```bash
# Pod별 리소스 사용량
kubectl top pods -n thock-prod

# Node별 리소스 사용량
kubectl top nodes
```

### HPA 상태 확인

```bash
kubectl get hpa -n thock-prod
```

### 이벤트 확인

```bash
kubectl get events -n thock-prod --sort-by='.lastTimestamp'
```

### 전체 삭제 (주의!)

```bash
# 네임스페이스 전체 삭제 (모든 리소스 삭제됨)
kubectl delete namespace thock-prod
```

---

## AWS EKS 특화 설정

### EKS 클러스터 생성

```bash
eksctl create cluster \
  --name thock-prod-cluster \
  --region ap-northeast-2 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 3 \
  --nodes-min 2 \
  --nodes-max 5 \
  --managed
```

### AWS Load Balancer Controller 설치

```bash
# IAM Policy 생성
curl -o iam-policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/main/docs/install/iam_policy.json
aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam-policy.json

# Helm으로 설치
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=thock-prod-cluster
```

### EBS CSI Driver 설치 (PersistentVolume 사용 시)

```bash
eksctl create iamserviceaccount \
  --name ebs-csi-controller-sa \
  --namespace kube-system \
  --cluster thock-prod-cluster \
  --attach-policy-arn arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy \
  --approve

eksctl create addon \
  --name aws-ebs-csi-driver \
  --cluster thock-prod-cluster
```

---

## 다음 단계

1. **CI/CD 파이프라인 구성**: GitHub Actions 또는 Jenkins를 사용한 자동 배포
2. **Helm Chart 전환**: 더 쉬운 버전 관리를 위해 Helm Chart로 패키징
3. **GitOps 도입**: ArgoCD를 사용한 선언적 배포
4. **Service Mesh**: Istio 또는 Linkerd 도입으로 고급 트래픽 관리
5. **보안 강화**: Network Policy, Pod Security Policy 적용

---

## 문의

문제가 발생하거나 질문이 있으시면 팀에 문의하세요.
