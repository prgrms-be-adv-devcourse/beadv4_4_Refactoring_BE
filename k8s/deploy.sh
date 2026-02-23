#!/bin/bash

# Kubernetes Deployment Script for Thock Application
# Usage: ./deploy.sh [--skip-secrets] [--skip-ingress] [--skip-monitoring]

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Kubernetes Deployment Script${NC}"
echo -e "${GREEN}========================================${NC}"

# Function to print step
print_step() {
    echo -e "\n${YELLOW}>>> $1${NC}\n"
}

# Function to wait for rollout
wait_for_rollout() {
    local resource_type=$1
    local resource_name=$2
    local namespace=$3

    echo "Waiting for $resource_type/$resource_name to be ready..."
    kubectl rollout status $resource_type/$resource_name -n $namespace --timeout=300s
}

# Function to wait for pod ready
wait_for_pod() {
    local label=$1
    local namespace=$2

    echo "Waiting for pod with label $label to be ready..."
    kubectl wait --for=condition=ready pod -l $label -n $namespace --timeout=300s
}

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}kubectl is not installed. Please install kubectl first.${NC}"
    exit 1
fi

# Check if we can connect to Kubernetes cluster
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Cannot connect to Kubernetes cluster. Please check your kubeconfig.${NC}"
    exit 1
fi

print_step "Step 1: Creating Namespace"
kubectl apply -f base/namespace.yaml

print_step "Step 2: Creating ConfigMaps"
kubectl apply -f base/configmap.yaml
kubectl apply -f database/mysql-configmap.yaml
kubectl apply -f monitoring/prometheus-config.yaml
kubectl apply -f monitoring/loki-config.yaml
kubectl apply -f monitoring/promtail-config.yaml
kubectl apply -f monitoring/grafana.yaml  # Grafana datasources ConfigMap included

print_step "Step 3: Creating Secrets"
if [ "$SKIP_SECRETS" = false ]; then
    if [ -f "base/secrets.yaml" ]; then
        kubectl apply -f base/secrets.yaml
        echo -e "${GREEN}Secrets created successfully${NC}"
    else
        echo -e "${RED}ERROR: base/secrets.yaml not found!${NC}"
        echo -e "${YELLOW}Please copy base/secrets.yaml.template to base/secrets.yaml and fill in the values.${NC}"
        echo -e "${YELLOW}Then run: kubectl apply -f base/secrets.yaml${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}Skipping secrets creation (--skip-secrets flag)${NC}"
fi

print_step "Step 4: Deploying MySQL Database"
kubectl apply -f database/mysql-statefulset.yaml
wait_for_pod "app=mysql" "thock-prod"

print_step "Step 5: Deploying Redpanda (Kafka)"
kubectl apply -f messaging/redpanda-statefulset.yaml
kubectl apply -f messaging/redpanda-console.yaml
wait_for_pod "app=redpanda" "thock-prod"

print_step "Step 6: Deploying Application Services"
kubectl apply -f services/member-service.yaml
kubectl apply -f services/product-service.yaml
kubectl apply -f services/payment-service.yaml
kubectl apply -f services/settlement-service.yaml
kubectl apply -f services/market-service.yaml
kubectl apply -f services/api-gateway.yaml

echo "Waiting for services to be ready..."
wait_for_rollout "deployment" "member-service" "thock-prod"
wait_for_rollout "deployment" "product-service" "thock-prod"
wait_for_rollout "deployment" "payment-service" "thock-prod"
wait_for_rollout "deployment" "settlement-service" "thock-prod"
wait_for_rollout "deployment" "market-service" "thock-prod"
wait_for_rollout "deployment" "api-gateway" "thock-prod"

print_step "Step 7: Deploying Monitoring Stack"
if [ "$SKIP_MONITORING" = false ]; then
    kubectl apply -f monitoring/prometheus.yaml
    kubectl apply -f monitoring/loki.yaml
    kubectl apply -f monitoring/promtail.yaml
    kubectl apply -f monitoring/grafana.yaml

    echo "Waiting for monitoring services to be ready..."
    wait_for_rollout "deployment" "prometheus" "thock-prod"
    wait_for_rollout "deployment" "loki" "thock-prod"
    wait_for_rollout "deployment" "grafana" "thock-prod"
else
    echo -e "${YELLOW}Skipping monitoring deployment (--skip-monitoring flag)${NC}"
fi

print_step "Step 8: Deploying Ingress"
if [ "$SKIP_INGRESS" = false ]; then
    # Check if cert-manager is installed
    if kubectl get namespace cert-manager &> /dev/null; then
        echo "Cert-manager found, creating cert issuers..."
        kubectl apply -f ingress/cert-issuer.yaml
    else
        echo -e "${YELLOW}WARNING: cert-manager not found. Skipping cert-issuer creation.${NC}"
        echo -e "${YELLOW}To install cert-manager, run:${NC}"
        echo -e "${YELLOW}kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml${NC}"
    fi

    kubectl apply -f ingress/ingress.yaml
else
    echo -e "${YELLOW}Skipping ingress deployment (--skip-ingress flag)${NC}"
fi

print_step "Step 9: Deploying HPA (Horizontal Pod Autoscaler)"
# Check if metrics-server is installed
if kubectl get deployment metrics-server -n kube-system &> /dev/null; then
    kubectl apply -f base/hpa.yaml
    echo -e "${GREEN}HPA created successfully${NC}"
else
    echo -e "${YELLOW}WARNING: metrics-server not found. Skipping HPA creation.${NC}"
    echo -e "${YELLOW}To install metrics-server, run:${NC}"
    echo -e "${YELLOW}kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml${NC}"
fi

print_step "Deployment Complete!"
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All resources have been deployed!${NC}"
echo -e "${GREEN}========================================${NC}"

print_step "Checking Status..."
echo "Pods:"
kubectl get pods -n thock-prod

echo -e "\nServices:"
kubectl get svc -n thock-prod

echo -e "\nIngress:"
kubectl get ingress -n thock-prod

echo -e "\n${GREEN}Useful Commands:${NC}"
echo -e "  View pods:              ${YELLOW}kubectl get pods -n thock-prod${NC}"
echo -e "  View services:          ${YELLOW}kubectl get svc -n thock-prod${NC}"
echo -e "  View logs:              ${YELLOW}kubectl logs -f <pod-name> -n thock-prod${NC}"
echo -e "  Describe pod:           ${YELLOW}kubectl describe pod <pod-name> -n thock-prod${NC}"
echo -e "  Port-forward (Grafana): ${YELLOW}kubectl port-forward svc/grafana 3000:3000 -n thock-prod${NC}"
echo -e "  Get HPA status:         ${YELLOW}kubectl get hpa -n thock-prod${NC}"