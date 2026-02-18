# Microservices Architecture Q&A

A collection of questions and answers about microservices architecture, Spring Boot threading, API Gateway, distributed tracing, and inter-service communication.

---

## Table of Contents
1. [@EnableCircuitBreaker Questions](#enablecircuitbreaker-questions)
2. [Spring Threading Questions](#spring-threading-questions)
3. [API Gateway Questions](#api-gateway-questions)
4. [Distributed Tracing Questions](#distributed-tracing-questions)
5. [Inter-Service Communication Questions](#inter-service-communication-questions)
6. [Reactive Programming Questions](#reactive-programming-questions)

---

## @EnableCircuitBreaker Questions

### Q1: Why did you add @EnableCircuitBreaker?

**A:** I initially added `@EnableCircuitBreaker` when implementing full security and fault tolerance features. This annotation enables method-level circuit breaker support using `@CircuitBreaker` and `@Retry` annotations on service methods.

However, when we simplified the project to a demo mode (removed JWT security), I also removed `@EnableCircuitBreaker` because:

1. **Auto-configuration in `application.yml`** already enables circuit breaker for WebClient and OpenFeign
2. **Method-level annotations** aren't needed for your current use case
3. **Simpler is better for demo** - less complex code

---

### Q2: Do I need @EnableCircuitBreaker now?

**A:** NO. Your current configuration in `application.yml` automatically configures circuit breaker:

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
    instances:
      accountsService:
        base-config: default
```

This is applied automatically to WebClient calls, no annotation needed.

---

### Q3: When would I need @EnableCircuitBreaker?

**A:** You need `@EnableCircuitBreaker` when:

| Scenario | Why Needed |
|----------|-------------|
| Using `@CircuitBreaker` annotation on methods | To enable the annotation processor |
| Using `@Retry` annotation on methods | To enable retry logic |
| Using `@TimeLimiter` annotation on methods | To enable timeout logic |
| Using method-level AOP-style cross-cutting | To apply circuit breaker via AOP |

**Example of when needed:**
```java
@EnableCircuitBreaker
@Service
public class AccountService {

    @CircuitBreaker(name = "database", fallbackMethod = "fallback")
    @Retry(maxAttempts = 3)
    @TimeLimiter(name = "db")
    public Account getAccount(Long id) {
        // Circuit breaker applied automatically
        return repository.findById(id);
    }

    public Account fallback(Long id) {
        // Called when circuit is OPEN
        return Account.defaultAccount();
    }
}
```

---

## Spring Threading Questions

### Q1: How does Spring Boot handle multiple concurrent requests?

**A:** Spring Boot uses a **thread pool model** (in blocking mode with Spring MVC + Tomcat):

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Request Handling Flow                               │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  1. REQUEST ARRIVES → HTTP Server accepts connection                  │
│  2. THREAD POOL (default: 200 threads) assigns a thread       │
│  3. THREAD processes request (Controller → Service → Repository)  │
│  4. RESPONSE SENT → Thread returns to pool, ready for next request │
│  5. NEXT REQUEST → Process repeats for next request               │
│                                                                            │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

**Key points:**
- Each request gets **one dedicated thread** from the pool
- Thread is **blocked** during the entire request processing
- When thread finishes, it returns to pool for reuse
- If pool is exhausted (200 threads all busy), new requests **wait in queue**
- Default thread pool: 200 threads (configurable in `application.yml`)

---

### Q2: What is the thread topology behind Spring Boot?

**A:** The thread topology in blocking mode (Spring MVC + Tomcat) consists of:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Thread Pool Architecture                            │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  HTTP SERVER (Tomcat)                                                 │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  THREAD POOL                                                     │      │
│  ├─────────────────────────────────────────────────────────────────────┤      │
│  │  Total Threads: 200 (configurable)                            │      │
│  │  ┌───────────────────────────────────────────────────────────────┐    │
│  │  │ Min Spare Threads: 10 (always available)                 │    │
│  │  │  ┌──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┐   │    │
│  │  │  T │ T │ T │ T │ T │ T │ T │ T │ T │ T │ T │ T │ T │   │    │
│  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │   │    │
│  │  └──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘   │    │
│  │      ↓                                                             │    │
│  │  READY THREADS (idle, waiting for work)                     │    │
│  └───────────────────────────────────────────────────────────────────────┘      │
│  │                                                                    │      │
│  ├─────────────────────────────────────────────────────────────────────┤      │
│  │  BUSY THREADS (processing requests)                           │      │
│  │  ┌───────────────────────────────────────────────────────────────┐    │
│  │  │  Thread #1: Processing Request A (BLOCKED on DB)      │    │
│  │  │  Thread #2: Processing Request B (BLOCKED on DB)      │    │
│  │  │  Thread #3: Processing Request C (BLOCKED on external)   │    │
│  │  │  Thread #4-N: Processing other requests...               │    │
│  │  │  ...                                                             │    │
│  │  └───────────────────────────────────────────────────────────────┘    │
│  │                                                                    │      │
│  │  When thread finishes → Returns to READY pool                │      │
│  │  When request arrives → Assigned to READY thread               │      │
│  │                                                                    │      │
│  └───────────────────────────────────────────────────────────────────────┘      │
│                                                                            │
│  DATABASE CONNECTION POOL (HikariCP)                                  │      │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  Max Connections: 10 (configurable)                            │      │
│  │  ┌──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┐   │    │
│  │  │ C │ C │ C │ C │ C │ C │ C │ C │ C │ C │ C │ C │   │    │
│  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │  │   │    │
│  │  └──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘   │    │
│  │      ↓                                                             │    │
│  │  Active connections (in use by threads)                      │    │
│  └───────────────────────────────────────────────────────────────────────┘      │
│                                                                            │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

### Q3: What happens when all 200 threads are busy?

**A:** When all threads are busy processing requests:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Thread Exhaustion Scenario                       │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  STATUS:                                                                │
│  - 200 threads: BLOCKED on I/O (DB, HTTP, external services)      │
│  - 0 threads: Available for new requests                            │
│                                                                        │
│  NEW REQUEST ARRIVES:                                                  │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  Request #201                                                          │      │
│  │  └──────────┬───────────────────────────────────────────────────┘      │
│  │               │                                                     │      │
│  │               ▼                                                     │      │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  Thread Pool: All 200 threads BUSY                           │      │
│  │  └──────────┬───────────────────────────────────────────────────┘      │
│  │               │                                                     │      │
│  │               ▼                                                     │      │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  Request Queue (accept-queue, default 100)                   │      │
│  │  ┌───────────────────────────────────────────────────────────────┐     │
│  │  │ Request waits in queue...                                │     │
│  │  │                                                               │     │
│  │  │  Queue: [#201, #202, #203, #204, ...]               │     │
│  │  │                                                               │     │
│  │  └───────────────────────────────────────────────────────────────┘     │
│  │               │                                                     │      │
│  │               ▼                                                     │      │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  CLIENT:                                                              │      │
│  │  ┌───────────────────────────────────────────────────────────────┐     │
│  │  │ Connection accepted, waiting for processing...           │     │
│  │  │ May wait 5-10+ seconds (if threads slow)               │     │
│  │  └───────────────────────────────────────────────────────────────┘     │
│  │  └───────────────────────────────────────────────────────────────┘      │
│                                                                            │
│  RESULT:                                                                │
│  - High latency for clients (queuing delay)                         │
│  - Potential timeouts (if queue wait too long)                      │
│  - Degraded performance                                       │
│                                                                            │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

**Solutions:**
1. **Increase thread pool size**: `server.tomcat.threads.max=500`
2. **Switch to reactive**: Event loop can handle 1000s of concurrent requests
3. **Add circuit breaker**: Fail fast instead of waiting on slow services

---

### Q4: How many threads does Spring Boot use by default?

**A:** Default thread pool sizes (Spring Boot 3.x with embedded Tomcat):

| Property | Default Value | Description |
|----------|---------------|-------------|
| `server.tomcat.threads.max` | 200 | Maximum concurrent requests |
| `server.tomcat.threads.min-spare` | 10 | Min idle threads in pool |
| `server.tomcat.accept-count` | 100 | Max connections in accept queue |
| `server.tomcat.max-connections` | 10000 | Maximum connections accepted |

**Configuration example:**
```yaml
server:
  tomcat:
    threads:
      max: 500              # Increase for high concurrency
      min-spare: 20          # More threads ready for spikes
    max-connections: 10000  # Total connections accepted
    accept-count: 200        # Max waiting in queue
```

---

## API Gateway Questions

### Q1: How does API Gateway route requests to microservices?

**A:** API Gateway uses **predicate-based routing** with the following flow:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    API Gateway Routing Flow                        │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  STEP 1: Request arrives at Gateway                                │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  Client Request:                                                  │      │
│  │  GET http://gateway:9090/api/v1/accounts/fetch?mobile=123│      │
│  │                                                                   │      │
│  └──────────┬───────────────────────────────────────────────────────────┘      │
│               │                                                     │      │
│               ▼                                                     │      │
│                                                                        │
│  STEP 2: Predicates evaluated                                        │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  Check all configured routes:                                     │      │
│  ├─────────────────────────────────────────────────────────────────────┤      │
│  │ Route 1: /api/v1/accounts/**                                  │      │
│  │  ┌───────────────────────────────────────────────────────────────┐   │
│  │  │ Predicate 1: Path=/api/v1/accounts/**                │   │
│  │  │  └──────────┬───────────────────────────────────────────────────┘   │
│  │  │               │ Request path matches? YES ✓                   │   │
│  │  │               └──────────┬───────────────────────────────────────────┘   │
│  │  │                          ▼                                      │   │
│  │  ┌───────────────────────────────────────────────────────────────┐   │
│  │  │ ROUTE MATCHED: accounts-service                        │   │
│  │  └───────────────────────────────────────────────────────────────┘   │
│  │                                                                       │   │
│  Route 2: /api/v1/loans/** (not matched)                 │   │
│  │  ┌───────────────────────────────────────────────────────────────┐   │
│  │  │ Predicate: Path=/api/v1/loans/**                       │   │
│  │  └──────────┬───────────────────────────────────────────────────┘   │
│  │  │               │ Request path doesn't match                    │   │
│  └───────────────────────────────────────────────────────────────┘   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────┘      │
│               │                                                     │      │
│               ▼                                                     │      │
│                                                                        │
│  STEP 3: Resolve target service (Service Discovery + Load Balancing)  │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  URI: lb://accounts-service                                  │      │
│  │                                                                   │      │
│  │  Query Eureka for available instances:                         │      │
│  │  ┌───────────────────────────────────────────────────────────────┐   │
│  │  │ accounts-service: [                                      │   │
│  │  │  10.0.1.10:8080,                                  │   │
│  │  │  10.0.1.11:8080,                                  │   │
│  │  │  10.0.1.12:8080                                   │   │
│  │  │ ]                                                        │   │
│  │  └──────────┬───────────────────────────────────────────────────┘   │
│  │               │                                                     │   │
│  │               ▼                                                     │      │
│  │  ┌───────────────────────────────────────────────────────────────┐   │
│  │  Load Balancer (Round Robin):                                  │   │
│  │  ┌───────────────────────────────────────────────────────────────┐   │
│  │  │ Last used: #1                                           │   │
│  │  │ Next: #2 (SELECTED)                                    │   │
│  │  └───────────────────────────────────────────────────────────────┘   │
│  │               │                                                     │   │
│  │               ▼                                                     │      │
│  │  Target: http://10.0.1.11:8080                       │   │
│  └───────────────────────────────────────────────────────────────┘      │
│               │                                                     │      │
│               ▼                                                     │      │
│                                                                        │
│  STEP 4: Apply pre-filters                                        │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  • Logging: Log request with correlation ID                     │      │
│  │  • Rate Limiting: Check client quota (10 req/s)               │      │
│  │  • Circuit Breaker: Is accounts-service available?            │      │
│  │  • Add Headers: X-Request-ID, X-Forwarded-For               │      │
│  └───────────────────────────────────────────────────────────────┘      │
│               │                                                     │      │
│               ▼                                                     │      │
│                                                                        │
│  STEP 5: Forward request to service                                   │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  Forwarded HTTP Request:                                       │      │
│  │  GET http://10.0.1.11:8080/api/v1/accounts/fetch         │      │
│  │  ?mobileNumber=123                                           │      │
│  │  Headers:                                                     │      │
│  │    X-Request-ID: abc123                                       │      │
│  │    X-Correlation-ID: def456                                      │      │
│  │    X-Forwarded-For: gateway                                   │      │
│  └───────────────────────────────────────────────────────────────┘      │
│               │                                                     │      │
│               ▼                                                     │      │
│                                                                        │
│  STEP 6: Wait for response (non-blocking)                           │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  (Gateway thread is FREE to handle other requests!)           │      │
│  └───────────────────────────────────────────────────────────────┘      │
│               │                                                     │      │
│               ▼                                                     │      │
│                                                                        │
│  STEP 7: Apply post-filters                                        │
│  ┌───────────────────────────────────────────────────────────────────────┐      │
│  │  • Circuit Breaker: Update metrics (success/failure)           │      │
│  │  • Logging: Log response time                                   │      │
│  │  • Add Headers: X-Response-Time, X-Trace-ID              │      │
│  └───────────────────────────────────────────────────────────────┘      │
│               │                                                     │      │
│               ▼                                                     │      │
│                                                                        │
│  STEP 8: Return to client                                          │
│  ┌───────────────────────────────────────────────────────────────┐      │
│  │  HTTP 200 OK                                                   │      │
│  │  {                                                             │      │
│  │    "name": "John Doe",                                        │      │
│  │    ...                                                         │      │
│  │  }                                                             │      │
│  └───────────────────────────────────────────────────────────────┘      │
│               │                                                     │      │
│               ▼                                                     │      │
│                                                                        │
│  Gateway thread READY for next request!                             │
│                                                                        │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

### Q2: What's the difference between `lb://` and direct URL?

**A:**

| URI Format | Description | Example |
|-----------|-------------|---------|
| `lb://service-name` | Load balance via service discovery | `lb://accounts-service` |
| `http://host:port` | Direct URL (no load balancing) | `http://10.0.1.10:8080` |
| `https://host` | Direct HTTPS URL | `https://api.example.com` |

**When to use each:**

```
lb://service-name      →  Internal microservices with multiple instances (most common)
http://ip:port        →  Single instance or testing
https://domain         →  External APIs or third-party services
```

---

## Distributed Tracing Questions

### Q1: What's the difference between Zipkin and Correlation ID?

**A:** Comparison table:

| Feature | Correlation ID | Zipkin (Distributed Tracing) |
|---------|----------------|---------------------------|
| **Purpose** | Link logs across services | Link logs + analyze performance |
| **Implementation** | Single header (UUID) | Multiple headers + spans |
| **Timing Info** | No | Yes (milliseconds per span) |
| **Span Hierarchy** | No | Yes (parent-child relationships) |
| **Visualization** | Log search only | Timeline UI |
| **Performance Analysis** | No | Yes |
| **Overhead** | Minimal | Moderate |
| **Complexity** | Very Low | Moderate |
| **Best For** | Simple log tracing | Production monitoring |

**When to use:**
- **Correlation ID only**: Development, simple demo, just need log linkage
- **Zipkin/Full tracing**: Production, performance analysis, debugging slow requests

---

### Q2: Can I replace Zipkin with just Correlation ID?

**A:** YES, you can! This is actually simpler and lighter:

**Pros of Correlation ID only:**
- Very simple to implement (just a header and MDC)
- Minimal overhead (no trace collection, no storage)
- Easier to understand and debug
- Sufficient for most use cases (just linking logs)

**Cons of Correlation ID only:**
- No timing information (don't know where it's slow)
- No span hierarchy (don't see service dependencies)
- No performance analysis
- No visualization UI

**Implementation:** See `CORRELATION_ID_GUIDE.md` in your project.

---

## Inter-Service Communication Questions

### Q1: What's the best way to communicate between microservices?

**A:** Depends on your use case:

| Scenario | Recommended | Reason |
|----------|-------------|--------|
| **Simple internal calls** | OpenFeign | Easiest, built-in load balancing with Eureka |
| **High performance needed** | WebClient (reactive) | Non-blocking, efficient |
| **Legacy interop needed** | RestTemplate | Familiar, but deprecated |
| **gRPC/high-throughput** | gRPC | Binary, very efficient |
| **Async/event-driven** | Kafka/RabbitMQ | Decoupled, event-driven |

**For your Medo Bank demo:** OpenFeign is best - simple and works with Eureka.

---

### Q2: When should I use WebClient vs OpenFeign?

**A:**

| Use WebClient when: | Use OpenFeign when: |
|-------------------|---------------------|
| You need reactive/non-blocking | You want simple, declarative API |
| Async composition of multiple calls | You want built-in load balancing |
| External API calls | Internal microservice communication |
| Need more control/flexibility | You want simplicity |
| Building reactive APIs | Blocking codebase is OK |

---

## Reactive Programming Questions

### Q1: Why use reactive programming for multi-part operations?

**A:** Reactive programming solves the problem of **sequential blocking**:

**Blocking code (your current approach):**
```
Total Time = Step1 (50ms) + Step2 (40ms) + Step3 (30ms) = 120ms
Threads blocked during all operations, can't handle other requests.
```

**Reactive code:**
```
Total Time = max(Step1, Step2, Step3) = max(50, 40, 30) = 50ms
All operations run in parallel, thread is free for other requests.
```

**For operations like:**
- DB query + external service call + calculation
- Multiple external service calls
- Streaming large result sets
- High concurrency requirements

Reactive provides 2-4x performance improvement in these scenarios.

---

### Q2: Is reactive programming worth the complexity?

**A:** For high-traffic microservices, YES. For simple apps, NO.

**Use reactive when:**
- You need to handle 1000+ concurrent requests
- I/O-bound operations (DB, HTTP, messaging)
- Streaming data
- Event-driven architecture

**Stick with blocking when:**
- Simple CRUD application (like your current demo)
- Low traffic (< 100 req/s)
- CPU-heavy operations
- Team is unfamiliar with reactive programming

---

## Quick Reference

### Your Current Stack (Spring MVC + Tomcat + Blocking)

| Layer | Technology | Threading Model |
|--------|-------------|-----------------|
| HTTP Server | Tomcat | Thread pool (200 threads) |
| Framework | Spring MVC | Synchronous (blocking) |
| Database | JPA/Hibernate | Blocking JDBC |
| External Calls | OpenFeign | Synchronous (blocking) |
| Throughput | ~10-50 req/s | Limited by thread pool |
| Complexity | Low | Familiar |

### Alternative Stack (Spring WebFlux + Netty + Reactive)

| Layer | Technology | Threading Model |
|--------|-------------|-----------------|
| HTTP Server | Netty | Event loop (1 thread + worker pool) |
| Framework | Spring WebFlux | Reactive (non-blocking) |
| Database | R2DBC | Reactive |
| External Calls | WebClient | Reactive |
| Throughput | ~1000+ req/s | Limited by I/O |
| Complexity | Medium | Requires reactive mindset |

### Decision Guide

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Migration Decision                            │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  Question 1: What's your traffic load?                            │
│     ┌─────────────┬─────────────┐                                       │
│     │ Low/Medium │ High       │                                       │
│     │   < 100/s  │  > 100/s   │                                       │
│     └─────────────┴─────────────┘                                       │
│           ↓               ↓                                                   │
│  Stay with      Switch to                                                │
│  Blocking       Reactive                                                  │
│  (Simple)      (Scalable)                                               │
│                                                                        │
│  Question 2: Do you need streaming data?                            │
│     ┌─────────────┬─────────────┐                                       │
│     │ No          │ Yes        │                                       │
│     └─────────────┴─────────────┘                                       │
│           ↓               ↓                                                   │
│  Stay with      Switch to                                                │
│  Blocking       Reactive                                                  │
│                                                                        │
│  Question 3: Team experience with reactive?                    │
│     ┌─────────────┬─────────────┐                                       │
│     │ None/Low    │ High       │                                       │
│     └─────────────┴─────────────┘                                       │
│           ↓               ↓                                                   │
│  Stay with      Switch to                                                │
│  Blocking       Reactive                                                  │
│                                                                        │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Configuration Reference

### Thread Pool Configuration

```yaml
# For Spring MVC (current)
server:
  tomcat:
    threads:
      max: 200              # Max concurrent requests
      min-spare: 10          # Min idle threads
    max-connections: 10000  # Max accepted connections
    accept-count: 100        # Max in accept queue

# For WebFlux (alternative)
server:
  netty:
    connection-timeout: 60s  # Connection timeout
    worker-threads: 8        # Event loop worker threads
```

### Circuit Breaker Configuration

```yaml
# Already in your application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
    instances:
      accountsService:
        base-config: default
```

### WebClient Configuration

```java
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .baseUrl("http://loans-service:8081")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector());
    }
}
```

---

## Summary

| Question | Quick Answer |
|----------|--------------|
| **Why @EnableCircuitBreaker?** | Was for method-level annotations, removed with JWT |
| **How Spring handles requests?** | Thread pool (200 threads), one per request |
| **When all threads busy?** | New requests wait in queue (latency) |
| **Blocking vs Reactive?** | Blocking: 10-50 req/s, Reactive: 1000+ req/s |
| **Best comm approach?** | OpenFeign for simple, WebClient for complex |
| **Zipkin vs Correlation ID?** | Correlation ID: simple, Zipkin: full tracing |
