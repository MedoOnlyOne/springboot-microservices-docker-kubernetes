# Medo Bank Microservices - Implementation Guide

This document provides a comprehensive guide to all microservices improvements implemented in this project.

**NOTE:** This is a demo project. All endpoints are public - no authentication required. Focus is on microservices architecture patterns.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Service Discovery (Eureka)](#1-service-discovery-eureka)
3. [API Gateway](#2-api-gateway-spring-cloud-gateway)
4. [Docker Compose Orchestration](#3-docker-compose-orchestration)
5. [Kubernetes Deployment](#4-kubernetes-manifests)
6. [Circuit Breaker (Resilience4j)](#5-circuit-breaker-resilience4j)
7. [Distributed Tracing (Sleuth + Zipkin)](#6-distributed-tracing-sleuth--zipkin)
8. [Database Migrations (Flyway)](#7-database-migrations-flyway)
9. [Quick Start](#quick-start)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        EXTERNAL CLIENT                          │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API GATEWAY (9090)                         │
│  • Routing            • Load Balancing                         │
│  • Rate Limiting       • Circuit Breaker                        │
│  • CORS               • Request Logging                        │
└─────────┬─────────────────────┬─────────────────┬────────────────┘
          │                     │                 │
          ▼                     ▼                 ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│  ACCOUNTS SERVICE │   │   EUREKA SERVER  │   │      ZIPKIN      │
│      (8080)      │   │     (8761)       │   │     (9411)       │
│  • No Auth       │   │ • Service Disco. │   │ • Tracing UI     │
│  • CRUD API      │   │ • Health Checks  │   │ • Trace Storage  │
│  • Business Logic│   │                 │   │                  │
└────────┬─────────┘   └──────────────────┘   └──────────────────┘
         │
         ▼
┌──────────────────┐
│     MySQL        │
│     (3306)       │
│  • Data Storage  │
│  • Persistence   │
└──────────────────┘
```

---

## 1. Service Discovery (Eureka)

### Purpose
Enables microservices to discover and communicate with each other without hardcoded URLs.

### How It Works

```
┌────────────────┐     Register     ┌─────────────────┐
│ Accounts Svc   │─────────────────>│   Eureka Server │
│   (Client)     │<─────────────────│    (Registry)   │
└────────┬───────┘   Heartbeat      └────────┬────────┘
         │                              │
         │ Fetch Registry                │
         ▼                              │
┌────────────────┐                      │
│ Gateway Svc    │◄─────────────────────┘
│   (Client)     │   Gets: {accounts-service: [10.0.1.5:8080]}
└────────────────┘
```

### Implementation Details

**Files:**
- `EurekaServer/pom.xml` - Eureka Server dependencies
- `EurekaServer/src/main/resources/application.yml` - Configuration
- `EurekaServer/src/main/java/com/medo/eureka/EurekaServerApplication.java`
- `Accounts/pom.xml` - Eureka Client dependencies
- `Accounts/src/main/resources/application.yml` - Client configuration

**Key Configuration:**
```yaml
eureka:
  instance:
    instance-id: ${spring.application.name}:${server.port}:${random.value}
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30  # Heartbeat every 30s
    lease-expiration-duration-in-seconds: 90  # Remove after 90s no heartbeat
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
```

### Benefits
- Dynamic service discovery
- Automatic load balancing
- Self-healing (removes dead instances)
- No hardcoded service URLs

---

## 2. API Gateway (Spring Cloud Gateway)

### Purpose
Single entry point for all microservices with cross-cutting concerns.

### How It Works

```
Client Request ──┐
                │
                ▼
         ┌──────────────┐
         │  Gateway     │
         │  - Routing   │
         │  - Filtering │
         └──────┬───────┘
                │
      ┌─────────┼─────────┐
      │         │         │
      ▼         ▼         ▼
  Accounts   Loans    Cards
```

### Implementation Details

**Files:**
- `ApiGateway/pom.xml`
- `ApiGateway/src/main/resources/application.yml`
- `ApiGateway/src/main/java/com/medo/gateway/filter/LoggingFilter.java`
- `ApiGateway/src/main/java/com/medo/gateway/filter/FallbackHandler.java`

**Route Configuration:**
```yaml
routes:
  - id: accounts-service
    uri: lb://accounts-service  # Load-balanced via Eureka
    predicates:
      - Path=/api/v1/accounts/**
    filters:
      - CircuitBreaker
      - RequestRateLimiter
      - Retry
```

### Gateway Features

| Feature | Purpose |
|---------|---------|
| **Routing** | Maps URLs to backend services |
| **Load Balancing** | Distributes traffic across instances |
| **Circuit Breaker** | Prevents cascading failures |
| **Rate Limiting** | Controls request rate per client |
| **Retry** | Retries failed requests |
| **CORS** | Handles cross-origin requests |
| **Logging** | Logs all requests/responses |

---

## 4. Docker Compose Orchestration

### Purpose
Orchestrates all services for local development.

### Implementation Details

**File:** `docker-compose.yml`

**Services:**
| Service | Port | Purpose |
|---------|------|---------|
| mysql | 3306 | Database |
| eureka-server | 8761 | Service discovery |
| accounts-service | 8080 | Accounts API |
| api-gateway | 9090 | Single entry point |
| zipkin | 9411 | Distributed tracing |
| elasticsearch | 9200 | Trace storage |
| prometheus | 9092 | Metrics collection |

### Usage
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild and start
docker-compose up -d --build
```

### Dockerfile Features
- Multi-stage builds (smaller images)
- Security: non-root user
- Health checks
- Resource limits
- Volume mounts for persistence

---

## 5. Kubernetes Manifests

### Purpose
Production-ready deployment with auto-scaling and high availability.

### Implementation Details

**Directory Structure:**
```
k8s/
├── namespace.yaml           # Create namespace
├── ingress.yaml            # External access
├── mysql/                  # MySQL deployment
├── eureka/                 # Eureka Server deployment
├── accounts/               # Accounts Service deployment
├── gateway/                # API Gateway deployment
├── zipkin/                 # Zipkin deployment
└── deploy.sh               # Deployment script
```

### Kubernetes Resources

| Resource | Purpose |
|----------|---------|
| **Deployment** | Manages pod replicas |
| **Service** | Exposes pods internally |
| **Ingress** | External access with routing |
| **ConfigMap** | Configuration management |
| **Secret** | Sensitive data (passwords) |
| **PVC** | Persistent storage |
| **HPA** | Auto-scale based on CPU/Memory |

### Deployment
```bash
# Deploy all services
./k8s/deploy.sh deploy

# Delete all services
./k8s/deploy.sh delete

# Check status
./k8s/deploy.sh status
```

### Auto-Scaling
```yaml
minReplicas: 2
maxReplicas: 5
metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        averageUtilization: 70
```

---

## 6. Circuit Breaker (Resilience4j)

### Purpose
Prevents cascading failures and provides fault tolerance.

### How It Works

```
┌─────────────┐
│   Gateway   │
└──────┬──────┘
       │ Request
       ▼
┌─────────────────────────────────┐
│      Circuit Breaker            │
│  ┌───────────────────────────┐  │
│  │ CLOSED                    │  │   (Normal operation)
│  │  • Requests pass through  │  │
│  │  • Monitoring failures    │  │
│  └───────────────┬───────────┘  │
│                  │ 50% failure   │
│                  ▼              │
│  ┌───────────────────────────┐  │
│  │ OPEN                      │  │   (Circuit tripped)
│  │  • Requests blocked       │  │
│  │  • Fallback triggered     │  │
│  │  • Wait 10s               │  │
│  └───────────────┬───────────┘  │
│                  │ 10s elapsed   │
│                  ▼              │
│  ┌───────────────────────────┐  │
│  │ HALF-OPEN                 │  │   (Testing recovery)
│  │  • Allow 3 test requests  │  │
│  │  • If success → CLOSED    │  │
│  │  • If fail → OPEN         │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

### Configuration
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

### Benefits
- Prevents cascading failures
- Fast failure (no waiting on hung services)
- Automatic recovery detection
- Configurable thresholds

---

## 7. Distributed Tracing (Sleuth + Zipkin)

### Purpose
Trace requests across microservices for debugging and performance analysis.

### How It Works

```
Request ──┬─> Gateway ──┬─> Accounts
          │             └─> Database
          │
          └─> Zipkin UI
```

### Trace Data
```
Trace ID: 7b8a1f2c3d4e5f6a (shared across services)
Span:
  ├─ Gateway (5ms)
  │   ├─ Authentication (2ms)
  │   └─ Routing (1ms)
  ├─ Accounts (50ms)
  │   ├─ Database Query (40ms)
  │   └─ Business Logic (5ms)
  └─ Total: 55ms
```

### Implementation
```yaml
spring.sleuth:
  zipkin:
    base-url: http://zipkin:9411
  sampling:
    probability: 1.0  # 100% sampling
```

### Usage
1. Navigate to Zipkin UI: `http://localhost:9411`
2. Search by Trace ID, Service, or Duration
3. View detailed trace timeline
4. Identify bottlenecks and slow services

---

## 8. Database Migrations (Flyway)

### Purpose
Version-controlled database schema changes.

### How It Works

```
┌─────────────────────────────────────┐
│   Flyway                           │
│  ┌───────────────────────────────┐ │
│  │ V1__Create_customers_table.sql │ │  ← Applied
│  │ V2__Create_accounts_table.sql │ │  ← Applied
│  │ V3__Create_indexes.sql         │ │  ← Applied
│  │ V4__Add_new_column.sql         │ │  ← Pending
│  └───────────────────────────────┘ │
│                                     │
│  Schema Version Table               │
│  ┌───────────────────────────────┐ │
│  │ version | description        │ │
│  │    1    | Create customers   │ │
│  │    2    | Create accounts    │ │
│  │    3    | Create indexes     │ │
│  └───────────────────────────────┘ │
└─────────────────────────────────────┘
```

### Migration Files
```
db/migration/
├── V1__Create_customers_table.sql
├── V2__Create_accounts_table.sql
└── V3__Create_indexes_and_triggers.sql
```

### Benefits
- Version-controlled schema changes
- Reproducible database state
- Team collaboration support
- Rollback capability
- Automatic verification

---

## Quick Start

### Prerequisites
- Java 21
- Maven 3.9+
- Docker
- Docker Compose
- kubectl (for Kubernetes deployment)

### Local Development (Docker Compose)

```bash
# 1. Start all services
docker-compose up -d

# 2. Wait for services to start (check logs)
docker-compose logs -f

# 3. Access services
#   - API Gateway: http://localhost:9090
#   - Accounts Service: http://localhost:8080
#   - Eureka Dashboard: http://localhost:8761
#   - Zipkin UI: http://localhost:9411
#   - Prometheus: http://localhost:9092

# 4. Create account (no auth required - demo mode)
curl -X POST http://localhost:8080/api/v1/accounts/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "mobileNumber": "1234567890",
    "accountsDto": {
      "accountType": "Savings",
      "branchAddress": "123 Main St"
    }
  }'

# 5. Fetch account (no auth required - demo mode)
curl -X GET "http://localhost:8080/api/v1/accounts/fetch?mobileNumber=1234567890"

# 6. Update account
curl -X PUT http://localhost:8080/api/v1/accounts/update \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Updated",
    "email": "john.updated@example.com",
    "mobileNumber": "1234567890",
    "accountsDto": {
      "accountNumber": 1234567890,
      "accountType": "Current",
      "branchAddress": "456 Oak St"
    }
  }'
```

### Production Deployment (Kubernetes)

```bash
# 1. Build Docker images
docker build -t medo/eureka-server:latest ./EurekaServer
docker build -t medo/accounts-service:latest ./Accounts
docker build -t medo/api-gateway:latest ./ApiGateway

# 2. Push to registry (example)
docker push medo/eureka-server:latest
docker push medo/accounts-service:latest
docker push medo/api-gateway:latest

# 3. Deploy to Kubernetes
./k8s/deploy.sh deploy

# 4. Check deployment status
kubectl get all -n medo-bank

# 5. Port forward for local testing
kubectl port-forward -n medo-bank svc/api-gateway 9090:9090
```

### Monitoring and Observability

```bash
# View logs
kubectl logs -n medo-bank -f deployment/accounts-service

# Check health
curl http://localhost:9090/actuator/health

# View metrics
curl http://localhost:9090/actuator/prometheus

# View traces in Zipkin UI
open http://localhost:9411

# View metrics in Prometheus UI
open http://localhost:9092
```

---

## Architecture Benefits Summary

| Improvement | Benefit |
|------------|---------|
| Service Discovery | Dynamic service lookup, no hardcoded URLs |
| API Gateway | Single entry point, cross-cutting concerns |
| Docker Compose | Easy local development |
| Kubernetes | Production-ready, auto-scaling |
| Circuit Breaker | Fault tolerance, prevents cascading failures |
| Distributed Tracing | Debug distributed systems |
| Flyway | Version-controlled database migrations |

**Note:** This demo uses no authentication for simplicity. For production, add OAuth2/JWT as needed.

---

## Next Steps

1. Add additional microservices (Loans, Cards)
2. Implement Config Server for centralized configuration
3. Add authentication (JWT/OAuth2) for production
4. Implement Kafka for event-driven architecture
5. Add Grafana for visualization
6. Set up CI/CD pipeline
7. Implement chaos testing
