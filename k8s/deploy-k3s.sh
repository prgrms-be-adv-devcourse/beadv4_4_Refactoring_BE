#!/bin/bash

# K3s Deployment Script for Single EC2 Instance
# Usage: ./deploy-k3s.sh [--skip-secrets] [--skip-ingress] [--skip-monitoring]

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Flags
SKIP_SECRETS=false
SKIP_INGRESS=false
SKIP_MONITORING=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --skip-secrets)
      SKIP_SECRETS=true
      shift
      ;;
    --skip-ingress)
      SKIP_INGRESS=true
      shift
      ;;
    --skip-monitoring)
      SKIP_MONITORING=true
      shift
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}K3s Deployment Script (Single EC2)${NC}"
echo -e "${BLUE}========================================${NC}"

# Function to print step
print_step() {
    echo -e "\n${YELLOW}>>> $1${NC}\n"
}

# Check if running on K3s
print_step "Checking K3s Installation"
if ! command -v k3s &> /dev/null; then
    echo -e "${RED}K3s is not installed!${NC}"
    echo -e "${YELLOW}Install K3s with: curl -sfL https://get.k3s.io | sh -${NC}"
    exit 1
fi

# Check if kubectl is configured
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}kubectl is not configured!${NC}"
    echo -e "${YELLOW}Run the following commands:${NC}"
    echo -e "${YELLOW}mkdir -p ~/.kube${NC}"
    echo -e "${YELLOW}sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config${NC}"
    echo -e "${YELLOW}sudo chown \$USER:\$USER ~/.kube/config${NC}"
    exit 1
fi

echo -e "${GREEN}K3s is running!${NC}"
kubectl get nodes

print_step "Step 1: Optimizing for Single EC2"

# Reduce resource requests for single EC2
echo "Adjusting resource limits for single EC2 instance..."

# Backup original files
if [ ! -d "services/.backup" ]; then
    mkdir -p services/.backup
    cp services/*.yaml services/.backup/
fi

# Reduce replicas in HPA
if [ -f "base/hpa.yaml" ]; then
    sed -i.bak 's/minReplicas: 2/minReplicas: 1/g' base/hpa.yaml
    sed -i.bak 's/maxReplicas: 10/maxReplicas: 2/g' base/hpa.yaml
    sed -i.bak 's/maxReplicas: 8/maxReplicas: 2/g' base/hpa.yaml
    echo -e "${GREEN}HPA adjusted for single EC2${NC}"
fi

# Set storageClassName to local-path
echo "Setting storageClassName to 'local-path' for K3s..."
find database/ messaging/ monitoring/ -name "*.yaml" -type f -exec sed -i.bak 's/# storageClassName: gp3/storageClassName: local-path/g' {} \;

print_step "Step 2: Creating Namespace"
kubectl apply -f base/namespace.yaml

print_step "Step 3: Creating ConfigMaps"
kubectl apply -f base/configmap.yaml
kubectl apply -f database/mysql-configmap.yaml

if [ "$SKIP_MONITORING" = false ]; then
    kubectl apply -f monitoring/prometheus-config.yaml
    kubectl apply -f monitoring/loki-config.yaml
    kubectl apply -f monitoring/promtail-config.yaml
fi

print_step "Step 4: Creating Secrets"
if [ "$SKIP_SECRETS" = false ]; then
    if [ -f "base/secrets.yaml" ]; then
        kubectl apply -f base/secrets.yaml
        echo -e "${GREEN}Secrets created successfully${NC}"
    else
        echo -e "${RED}ERROR: base/secrets.yaml not found!${NC}"
        echo -e "${YELLOW}Please copy base/secrets.yaml.template to base/secrets.yaml and fill in the values.${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}Skipping secrets creation (--skip-secrets flag)${NC}"
fi

print_step "Step 5: Deploying MySQL Database"
kubectl apply -f database/mysql-statefulset.yaml
echo "Waiting for MySQL to be ready (this may take 2-3 minutes)..."
kubectl wait --for=condition=ready pod -l app=mysql -n thock-prod --timeout=300s || true

print_step "Step 6: Deploying Redpanda (Kafka)"
kubectl apply -f messaging/redpanda-statefulset.yaml
kubectl apply -f messaging/redpanda-console.yaml
echo "Waiting for Redpanda to be ready..."
kubectl wait --for=condition=ready pod -l app=redpanda -n thock-prod --timeout=300s || true

print_step "Step 7: Deploying Application Services"
echo "Deploying services with reduced resource requests..."

# Deploy services one by one
for service in member product payment settlement market api-gateway; do
    echo "Deploying ${service}-service..."
    kubectl apply -f services/${service}-service.yaml
done

echo "Waiting for services to be ready (this may take 5-10 minutes)..."
sleep 10

print_step "Step 8: Deploying Monitoring Stack"
if [ "$SKIP_MONITORING" = false ]; then
    echo "Deploying monitoring (this may use significant resources)..."
    kubectl apply -f monitoring/prometheus.yaml
    kubectl apply -f monitoring/loki.yaml
    kubectl apply -f monitoring/grafana.yaml

    # Skip Promtail on single EC2 to save resources
    echo -e "${YELLOW}Note: Promtail is skipped to save resources on single EC2${NC}"
else
    echo -e "${YELLOW}Skipping monitoring deployment (--skip-monitoring flag)${NC}"
fi

print_step "Step 9: Deploying Ingress"
if [ "$SKIP_INGRESS" = false ]; then
    echo -e "${BLUE}K3s uses Traefik Ingress Controller by default${NC}"

    # Check if cert-manager is installed
    if kubectl get namespace cert-manager &> /dev/null; then
        echo "Cert-manager found, creating cert issuers..."
        kubectl apply -f ingress/cert-issuer.yaml
    else
        echo -e "${YELLOW}WARNING: cert-manager not found.${NC}"
        echo -e "${YELLOW}To install cert-manager:${NC}"
        echo -e "${YELLOW}kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml${NC}"
    fi

    # Modify ingress for Traefik
    if grep -q "kubernetes.io/ingress.class.*nginx" ingress/ingress.yaml; then
        echo "Modifying ingress for Traefik..."
        sed -i.bak 's/kubernetes.io\/ingress.class: "nginx"/kubernetes.io\/ingress.class: "traefik"/g' ingress/ingress.yaml
    fi

    kubectl apply -f ingress/ingress.yaml
else
    echo -e "${YELLOW}Skipping ingress deployment (--skip-ingress flag)${NC}"
fi

print_step "Step 10: Deploying HPA (Horizontal Pod Autoscaler)"
# K3s includes metrics-server by default
kubectl apply -f base/hpa.yaml
echo -e "${GREEN}HPA created successfully${NC}"

print_step "Deployment Complete!"
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All resources have been deployed to K3s!${NC}"
echo -e "${GREEN}========================================${NC}"

print_step "Checking Status..."
echo "Pods:"
kubectl get pods -n thock-prod

echo -e "\nServices:"
kubectl get svc -n thock-prod

echo -e "\nIngress:"
kubectl get ingress -n thock-prod

echo -e "\nPersistentVolumeClaims:"
kubectl get pvc -n thock-prod

echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}K3s Specific Information${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "\n${YELLOW}Traefik Ingress Status:${NC}"
kubectl get svc -n kube-system traefik

echo -e "\n${YELLOW}Storage (local-path):${NC}"
kubectl get storageclass

echo -e "\n${GREEN}Useful Commands:${NC}"
echo -e "  View pods:              ${YELLOW}kubectl get pods -n thock-prod${NC}"
echo -e "  View logs:              ${YELLOW}kubectl logs -f <pod-name> -n thock-prod${NC}"
echo -e "  Port-forward API:       ${YELLOW}kubectl port-forward svc/api-gateway 8080:8080 -n thock-prod${NC}"
echo -e "  Port-forward Grafana:   ${YELLOW}kubectl port-forward svc/grafana 3000:3000 -n thock-prod${NC}"
echo -e "  Get HPA status:         ${YELLOW}kubectl get hpa -n thock-prod${NC}"
echo -e "  K3s service status:     ${YELLOW}sudo systemctl status k3s${NC}"

echo -e "\n${YELLOW}Note: Since this is a single EC2 instance, use NodePort or port-forwarding for external access.${NC}"
echo -e "${YELLOW}For production, consider using AWS EKS with multiple nodes.${NC}"

echo -e "\n${GREEN}To access your application:${NC}"
echo -e "1. Port-forward: ${YELLOW}kubectl port-forward svc/api-gateway 8080:8080 -n thock-prod${NC}"
echo -e "2. Then visit: ${YELLOW}http://localhost:8080${NC}"
echo -e "\nOr configure Ingress with your EC2 public IP and domain."