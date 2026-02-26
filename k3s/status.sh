#!/bin/bash

# Kubernetes Status Check Script for Thock Application
# Usage: ./status.sh

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Kubernetes Status Check${NC}"
echo -e "${GREEN}========================================${NC}"

# Check if namespace exists
if ! kubectl get namespace thock-prod &> /dev/null; then
    echo -e "${RED}Namespace 'thock-prod' does not exist!${NC}"
    exit 1
fi

echo -e "\n${YELLOW}=== Pods ===${NC}"
kubectl get pods -n thock-prod -o wide

echo -e "\n${YELLOW}=== Services ===${NC}"
kubectl get svc -n thock-prod

echo -e "\n${YELLOW}=== Deployments ===${NC}"
kubectl get deployments -n thock-prod

echo -e "\n${YELLOW}=== StatefulSets ===${NC}"
kubectl get statefulsets -n thock-prod

echo -e "\n${YELLOW}=== PersistentVolumeClaims ===${NC}"
kubectl get pvc -n thock-prod

echo -e "\n${YELLOW}=== Ingress ===${NC}"
kubectl get ingress -n thock-prod

echo -e "\n${YELLOW}=== HPA (Horizontal Pod Autoscaler) ===${NC}"
kubectl get hpa -n thock-prod

echo -e "\n${YELLOW}=== Recent Events ===${NC}"
kubectl get events -n thock-prod --sort-by='.lastTimestamp' | tail -20

echo -e "\n${YELLOW}=== Resource Usage (if metrics-server is installed) ===${NC}"
if kubectl top nodes &> /dev/null; then
    echo "Node Resource Usage:"
    kubectl top nodes
    echo ""
    echo "Pod Resource Usage:"
    kubectl top pods -n thock-prod
else
    echo -e "${RED}Metrics server not installed. Install with:${NC}"
    echo "kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml"
fi

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Status check complete!${NC}"
echo -e "${GREEN}========================================${NC}"