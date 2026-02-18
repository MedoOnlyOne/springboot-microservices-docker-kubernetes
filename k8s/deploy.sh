#!/bin/bash

# =====================================================
# Kubernetes Deployment Script
# =====================================================
# PURPOSE: Deploy all Medo Bank microservices to Kubernetes
#
# HOW TO USE:
#   ./deploy.sh          # Deploy all services
#   ./deploy.sh delete   # Delete all services
#   ./deploy.sh status   # Show status of all services
# =====================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="medo-bank"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Functions
print_header() {
    echo -e "\n${GREEN}=====================================${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}=====================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check if kubectl is installed
check_prerequisites() {
    print_header "Checking Prerequisites"

    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi

    print_success "kubectl is installed"

    if ! command -v docker &> /dev/null; then
        print_error "docker is not installed. Please install docker first."
        exit 1
    fi

    print_success "docker is installed"

    # Check if cluster is accessible
    if kubectl cluster-info &> /dev/null; then
        print_success "Kubernetes cluster is accessible"
    else
        print_error "Cannot access Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi
}

# Create namespace
create_namespace() {
    print_header "Creating Namespace"

    if kubectl get namespace "$NAMESPACE" &> /dev/null; then
        print_info "Namespace $NAMESPACE already exists"
    else
        kubectl apply -f "$SCRIPT_DIR/namespace.yaml"
        print_success "Namespace $NAMESPACE created"
    fi
}

# Deploy MySQL
deploy_mysql() {
    print_header "Deploying MySQL"
    kubectl apply -f "$SCRIPT_DIR/mysql/"
    print_success "MySQL deployed"
}

# Deploy Eureka Server
deploy_eureka() {
    print_header "Deploying Eureka Server"
    kubectl apply -f "$SCRIPT_DIR/eureka/"
    print_success "Eureka Server deployed"
}

# Deploy Accounts Service
deploy_accounts() {
    print_header "Deploying Accounts Service"
    kubectl apply -f "$SCRIPT_DIR/accounts/"
    print_success "Accounts Service deployed"
}

# Deploy API Gateway
deploy_gateway() {
    print_header "Deploying API Gateway"
    kubectl apply -f "$SCRIPT_DIR/gateway/"
    print_success "API Gateway deployed"
}

# Deploy Zipkin
deploy_zipkin() {
    print_header "Deploying Zipkin"
    kubectl apply -f "$SCRIPT_DIR/zipkin/"
    print_success "Zipkin deployed"
}

# Deploy Ingress
deploy_ingress() {
    print_header "Deploying Ingress"
    kubectl apply -f "$SCRIPT_DIR/ingress.yaml"
    print_success "Ingress deployed"
}

# Delete all deployments
delete_all() {
    print_header "Deleting All Deployments"
    kubectl delete namespace "$NAMESPACE" --ignore-not-found=true
    print_success "All deployments deleted"
}

# Show status
show_status() {
    print_header "Deployment Status"
    kubectl get all -n "$NAMESPACE"
}

# Wait for deployments to be ready
wait_for_ready() {
    print_header "Waiting for Deployments to be Ready"

    echo "Waiting for MySQL..."
    kubectl wait --for=condition=ready pod -l app=mysql -n "$NAMESPACE" --timeout=120s || true

    echo "Waiting for Eureka Server..."
    kubectl wait --for=condition=ready pod -l app=eureka-server -n "$NAMESPACE" --timeout=120s || true

    echo "Waiting for Accounts Service..."
    kubectl wait --for=condition=ready pod -l app=accounts-service -n "$NAMESPACE" --timeout=120s || true

    echo "Waiting for API Gateway..."
    kubectl wait --for=condition=ready pod -l app=api-gateway -n "$NAMESPACE" --timeout=120s || true

    print_success "All deployments are ready"
}

# Show access information
show_access_info() {
    print_header "Access Information"

    echo -e "\nServices:"
    echo -e "  API Gateway:    http://api-gateway.$NAMESPACE.svc.cluster.local:9090"
    echo -e "  Accounts:       http://accounts-service.$NAMESPACE.svc.cluster.local:8080"
    echo -e "  Eureka:         http://eureka-server.$NAMESPACE.svc.cluster.local:8761"
    echo -e "  Zipkin:         http://zipkin.$NAMESPACE.svc.cluster.local:9411"

    echo -e "\nPort Forward (for local access):"
    echo -e "  kubectl port-forward -n $NAMESPACE svc/api-gateway 9090:9090"
    echo -e "  kubectl port-forward -n $NAMESPACE svc/eureka-server 8761:8761"
    echo -e "  kubectl port-forward -n $NAMESPACE svc/zipkin 9411:9411"

    echo -e "\nLogs:"
    echo -e "  kubectl logs -n $NAMESPACE -f deployment/accounts-service"
    echo -e "  kubectl logs -n $NAMESPACE -f deployment/api-gateway"

    echo -e "\nMetrics:"
    echo -e "  http://localhost:9090/metrics (after port forward)"
}

# Main execution
main() {
    case "${1:-deploy}" in
        deploy)
            check_prerequisites
            create_namespace
            deploy_mysql
            sleep 5
            deploy_eureka
            sleep 10
            deploy_accounts
            deploy_gateway
            deploy_zipkin
            deploy_ingress
            wait_for_ready
            show_status
            show_access_info
            ;;
        delete)
            delete_all
            ;;
        status)
            show_status
            ;;
        *)
            echo "Usage: $0 {deploy|delete|status}"
            exit 1
            ;;
    esac
}

main "$@"
