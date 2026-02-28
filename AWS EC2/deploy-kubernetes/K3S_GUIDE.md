# K3s 단일 EC2 배포 가이드

이 가이드는 **학습용**으로 단일 EC2 인스턴스에서 K3s를 사용하여 Kubernetes 환경을 구축하는 방법을 설명합니다.

## K3s란?

- **경량 Kubernetes**: Kubernetes의 절반 크기 (40MB vs 400MB)
- **단일 바이너리**: 설치가 매우 간단
- **IoT/Edge 최적화**: 리소스가 제한된 환경에서 실행 가능
- **완전한 Kubernetes**: 모든 Kubernetes 기능 제공

## 사전 요구사항

### EC2 사양
- **인스턴스 타입**: t3.medium 이상 (4GB RAM)
- **OS**: Ubuntu 22.04 LTS
- **디스크**: 20GB 이상
- **포트**: 80, 443 (Ingress), 6443 (Kubernetes API)

---

## 설치 단계

### 1. K3s 설치

```bash
# EC2에 SSH 접속
ssh ubuntu@your-ec2-ip

# K3s 설치 (1분)
curl -sfL https://get.k3s.io | sh -

# 설치 확인
sudo k3s kubectl get nodes
```

### 2. kubectl 설정

```bash
# kubectl을 sudo 없이 사용
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config

# 확인
kubectl get nodes
```

### 3. StorageClass 확인

K3s는 기본적으로 `local-path` StorageClass를 제공합니다.

```bash
kubectl get storageclass

# 출력:
# NAME                   PROVISIONER             RECLAIMPOLICY
# local-path (default)   rancher.io/local-path   Delete
```

PersistentVolumeClaim에서 자동으로 사용됩니다.

---

## K3s 전용 설정 변경

### 1. Ingress Controller

K3s는 기본적으로 **Traefik Ingress**를 포함합니다. Nginx Ingress를 사용하려면:

#### 옵션 A: Traefik 사용 (권장 - 기본 포함)

`ingress/ingress.yaml` 수정:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: thock-ingress
  namespace: thock-prod
  annotations:
    # Traefik 사용
    kubernetes.io/ingress.class: "traefik"
    # SSL 리다이렉트
    traefik.ingress.kubernetes.io/redirect-entry-point: https
    # Cert-Manager 사용 (선택)
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.thock.site
    secretName: thock-tls
  rules:
  - host: api.thock.site
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-gateway
            port:
              number: 8080
      - path: /grafana
        pathType: Prefix
        backend:
          service:
            name: grafana
            port:
              number: 3000
      - path: /redpanda
        pathType: Prefix
        backend:
          service:
            name: redpanda-console
            port:
              number: 8090
```

#### 옵션 B: Nginx Ingress 설치

```bash
# Traefik 비활성화하고 K3s 재설치
curl -sfL https://get.k3s.io | sh -s - --disable traefik

# Nginx Ingress 설치
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
```

### 2. Metrics Server

K3s는 기본적으로 **Metrics Server**를 포함합니다.

```bash
# 확인
kubectl get deployment metrics-server -n kube-system

# HPA가 바로 작동합니다!
kubectl apply -f base/hpa.yaml
```

### 3. LoadBalancer 서비스

K3s는 **ServiceLB**를 기본 포함합니다.

```yaml
# LoadBalancer 타입 서비스 사용 가능
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
spec:
  type: LoadBalancer  # K3s에서 자동으로 처리
  ports:
  - port: 80
    targetPort: 8080
```

**주의**: 단일 EC2이므로 실제 외부 IP는 할당되지 않습니다. EC2의 Public IP를 사용하세요.

---

## 배포 방법

### 1. Secret 파일 준비

```bash
cd ~/k3s

# Secret 템플릿 복사
cp base/secrets.yaml.template base/secrets.yaml

# 비밀번호 입력
vim base/secrets.yaml
```

### 2. StorageClass 설정 (K3s용)

K3s는 `local-path`를 기본 StorageClass로 사용합니다.

각 PVC YAML 파일의 `storageClassName` 주석을 해제하고 수정:

```yaml
# database/mysql-statefulset.yaml
volumeClaimTemplates:
- metadata:
    name: mysql-storage
  spec:
    accessModes: ["ReadWriteOnce"]
    storageClassName: local-path  # K3s 기본값
    resources:
      requests:
        storage: 20Gi
```

**또는 자동으로 일괄 수정:**

```bash
# 모든 YAML 파일에서 storageClassName 주석 처리 부분을 local-path로 변경
find . -name "*.yaml" -type f -exec sed -i 's/# storageClassName: gp3/storageClassName: local-path/g' {} \;
```

### 3. 배포 실행

```bash
# 실행 권한 확인
chmod +x deploy.sh

# 배포
./deploy.sh
```

### 4. 배포 확인

```bash
# 상태 확인
./status.sh

# 또는
kubectl get pods -n thock-prod
kubectl get svc -n thock-prod
kubectl get pvc -n thock-prod
```

---

## 외부 접근 설정

### 방법 1: NodePort (간단, 학습용)

`services/api-gateway.yaml` 수정:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: thock-prod
spec:
  type: NodePort  # ClusterIP → NodePort
  selector:
    app: api-gateway
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30080  # 30000-32767 범위
```

```bash
# 적용
kubectl apply -f services/api-gateway.yaml

# 접속
http://your-ec2-public-ip:30080
```

### 방법 2: Ingress + 도메인

#### 2-1. 포트 포워딩 설정

K3s의 Traefik이 80/443 포트를 사용합니다.

```bash
# 80, 443 포트가 열려있는지 확인
sudo netstat -tlnp | grep -E ':(80|443)'

# Traefik 서비스 확인
kubectl get svc -n kube-system traefik

# 출력:
# NAME      TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)
# traefik   LoadBalancer   10.43.123.456   <EC2-IP>      80:xxxxx/TCP,443:xxxxx/TCP
```

#### 2-2. EC2 보안그룹 설정

AWS 콘솔에서:
- 인바운드 규칙 추가: TCP 80, 443 (0.0.0.0/0)

#### 2-3. 도메인 설정 (Route53)

```
A 레코드:
api.thock.site → your-ec2-public-ip
```

#### 2-4. Cert-Manager 설치 (SSL 자동 발급)

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Cert-Manager 준비 확인
kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=cert-manager -n cert-manager --timeout=300s
```

#### 2-5. Let's Encrypt Issuer 적용

```bash
kubectl apply -f ingress/cert-issuer.yaml
kubectl apply -f ingress/ingress.yaml
```

#### 2-6. SSL 인증서 확인

```bash
# 인증서 상태 확인 (5-10분 소요)
kubectl get certificate -n thock-prod

# READY가 True가 되면 완료
# NAME        READY   SECRET      AGE
# thock-tls   True    thock-tls   5m
```

#### 2-7. 접속

```
https://api.thock.site
```

---

## 유용한 K3s 명령어

### K3s 서비스 관리

```bash
# K3s 상태 확인
sudo systemctl status k3s

# K3s 재시작
sudo systemctl restart k3s

# K3s 중지
sudo systemctl stop k3s

# K3s 시작
sudo systemctl start k3s

# K3s 로그 확인
sudo journalctl -u k3s -f
```

### K3s 완전 삭제 (재설치 시)

```bash
# K3s 제거
/usr/local/bin/k3s-uninstall.sh

# 재설치
curl -sfL https://get.k3s.io | sh -
```

---

## 리소스 제한 조정 (단일 EC2용)

4GB RAM EC2에서는 리소스를 줄여야 합니다.

### 각 서비스의 리소스 제한 수정

```yaml
# services/*.yaml 파일들에서
resources:
  requests:
    memory: "256Mi"  # 512Mi → 256Mi로 감소
    cpu: "100m"      # 250m → 100m으로 감소
  limits:
    memory: "512Mi"  # 1Gi → 512Mi로 감소
    cpu: "500m"      # 1000m → 500m으로 감소
```

### HPA 설정 조정

```yaml
# base/hpa.yaml
spec:
  minReplicas: 1  # 2 → 1로 감소 (단일 EC2)
  maxReplicas: 2  # 10 → 2로 감소
```

**자동 수정 스크립트:**

```bash
# 리소스 요청/제한 감소
find services/ -name "*.yaml" -exec sed -i 's/memory: "512Mi"/memory: "256Mi"/g' {} \;
find services/ -name "*.yaml" -exec sed -i 's/cpu: "250m"/cpu: "100m"/g' {} \;
find services/ -name "*.yaml" -exec sed -i 's/memory: "1Gi"/memory: "512Mi"/g' {} \;
find services/ -name "*.yaml" -exec sed -i 's/cpu: "1000m"/cpu: "500m"/g' {} \;

# HPA 조정
sed -i 's/minReplicas: 2/minReplicas: 1/g' base/hpa.yaml
sed -i 's/maxReplicas: 10/maxReplicas: 2/g' base/hpa.yaml
sed -i 's/maxReplicas: 8/maxReplicas: 2/g' base/hpa.yaml
```

---

## 모니터링 스택 최적화

단일 EC2에서는 모든 모니터링 서비스를 실행하면 리소스가 부족할 수 있습니다.

### 옵션 1: 모니터링 제외 배포

```bash
./deploy.sh --skip-monitoring
```

### 옵션 2: Grafana만 사용

```bash
# Prometheus, Loki만 배포
kubectl apply -f monitoring/prometheus-config.yaml
kubectl apply -f monitoring/prometheus.yaml
kubectl apply -f monitoring/loki-config.yaml
kubectl apply -f monitoring/loki.yaml
kubectl apply -f monitoring/grafana.yaml

# Promtail 제외 (로그 수집 비활성화)
```

---

## 트러블슈팅

### 1. Pod가 Pending 상태

```bash
kubectl describe pod <pod-name> -n thock-prod

# 원인: 리소스 부족
# 해결: 리소스 요청 감소 또는 EC2 스펙 업그레이드
```

### 2. PVC가 Pending 상태

```bash
kubectl get pvc -n thock-prod
kubectl describe pvc <pvc-name> -n thock-prod

# 원인: StorageClass 문제
# 해결: storageClassName을 "local-path"로 설정
```

### 3. OOMKilled (메모리 부족)

```bash
kubectl get pods -n thock-prod

# STATUS가 OOMKilled인 경우
# 해결: 리소스 제한 증가 또는 불필요한 서비스 중단
```

### 4. Traefik Ingress가 작동하지 않음

```bash
# Traefik 상태 확인
kubectl get pods -n kube-system | grep traefik

# Traefik 로그 확인
kubectl logs -n kube-system <traefik-pod-name>

# Ingress 확인
kubectl describe ingress thock-ingress -n thock-prod
```

---

## Docker Compose로 다시 돌아가기

학습이 끝나고 Docker Compose로 돌아가려면:

```bash
# 1. Kubernetes 리소스 삭제
kubectl delete namespace thock-prod

# 2. K3s 제거
/usr/local/bin/k3s-uninstall.sh

# 3. Docker Compose 복구
cd /path/to/your/project
docker compose up -d
```

---

## 정리

### K3s의 장점 (학습용)
- ✅ 설치가 매우 간단 (1분)
- ✅ 단일 EC2에서 실행 가능
- ✅ 완전한 Kubernetes 경험
- ✅ 비용 절감 (기존 EC2 활용)

### K3s의 단점
- ❌ 진정한 고가용성 불가 (단일 노드)
- ❌ 프로덕션 사용 권장하지 않음
- ❌ 리소스 제약 (EC2 사양에 의존)

### 학습 후 다음 단계
1. K3s로 Kubernetes 개념 습득
2. 충분한 학습 후 EKS로 이전
3. 프로덕션 레벨 운영 경험

---

## 참고 자료

- K3s 공식 문서: https://docs.k3s.io/
- Kubernetes 공식 문서: https://kubernetes.io/ko/docs/
- Traefik 문서: https://doc.traefik.io/traefik/
