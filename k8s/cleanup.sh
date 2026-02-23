#!/bin/bash

# Kubernetes Cleanup Script for Thock Application
# Usage: ./cleanup.sh [--all]

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

DELETE_NAMESPACE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --all)
      DELETE_NAMESPACE=true
      shift
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

echo -e "${RED}========================================${NC}"
echo -e "${RED}Kubernetes Cleanup Script${NC}"
echo -e "${RED}========================================${NC}"

if [ "$DELETE_NAMESPACE" = true ]; then
    echo -e "${RED}WARNING: This will delete the entire thock-prod namespace and ALL resources!${NC}"
    read -p "Are you sure? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        echo "Cleanup cancelled."
        exit 0
    fi

    echo -e "${YELLOW}Deleting namespace thock-prod...${NC}"
    kubectl delete namespace thock-prod
    echo -e "${GREEN}Namespace deleted.${NC}"
else
    echo -e "${YELLOW}Deleting individual resources (namespace will be preserved)...${NC}"

    # Delete in reverse order
    echo "Deleting HPA..."
    kubectl delete -f base/hpa.yaml --ignore-not-found=true

    echo "Deleting Ingress..."
    kubectl delete -f ingress/ingress.yaml --ignore-not-found=true
    kubectl delete -f ingress/cert-issuer.yaml --ignore-not-found=true

    echo "Deleting Monitoring..."
    kubectl delete -f monitoring/grafana.yaml --ignore-not-found=true
    kubectl delete -f monitoring/promtail.yaml --ignore-not-found=true
    kubectl delete -f monitoring/loki.yaml --ignore-not-found=true
    kubectl delete -f monitoring/prometheus.yaml --ignore-not-found=true

    echo "Deleting Application Services..."
    kubectl delete -f services/api-gateway.yaml --ignore-not-found=true
    kubectl delete -f services/market-service.yaml --ignore-not-found=true
    kubectl delete -f services/settlement-service.yaml --ignore-not-found=true
    kubectl delete -f services/payment-service.yaml --ignore-not-found=true
    kubectl delete -f services/product-service.yaml --ignore-not-found=true
    kubectl delete -f services/member-service.yaml --ignore-not-found=true

    echo "Deleting Messaging..."
    kubectl delete -f messaging/redpanda-console.yaml --ignore-not-found=true
    kubectl delete -f messaging/redpanda-statefulset.yaml --ignore-not-found=true

    echo "Deleting Database..."
    kubectl delete -f database/mysql-statefulset.yaml --ignore-not-found=true

    echo "Deleting ConfigMaps..."
    kubectl delete -f monitoring/promtail-config.yaml --ignore-not-found=true
    kubectl delete -f monitoring/loki-config.yaml --ignore-not-found=true
    kubectl delete -f monitoring/prometheus-config.yaml --ignore-not-found=true
    kubectl delete -f database/mysql-configmap.yaml --ignore-not-found=true
    kubectl delete -f base/configmap.yaml --ignore-not-found=true

    echo -e "${GREEN}Cleanup complete!${NC}"
    echo -e "${YELLOW}Note: Namespace 'thock-prod' and Secrets were preserved.${NC}"
    echo -e "${YELLOW}To delete everything including the namespace, run: ./cleanup.sh --all${NC}"
fi

echo -e "\n${GREEN}Remaining resources:${NC}"
kubectl get all -n thock-prod