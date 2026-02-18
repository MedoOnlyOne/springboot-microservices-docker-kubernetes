# Correlation ID Implementation Guide

## Overview

Correlation ID is a simpler alternative to full distributed tracing. It allows you to trace a single request across all microservices by passing a unique identifier through the entire request chain.

## Implementation

### 1. API Gateway Filter

```java
// ApiGateway/src/main/java/com/medo/gateway/filter/CorrelationIdFilter.java
package com.medo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Correlation ID Filter
 *
 * PURPOSE: Generate or extract correlation ID and pass it through all services
 *
 * HOW IT WORKS:
 * 1. Extract X-Correlation-ID from incoming request (if present)
 * 2. If not present, generate new UUID
 * 3. Add to request header (passes to downstream services)
 * 4. Add to MDC for logging
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extract or generate correlation ID
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = generateCorrelationId();
        }

        // Add to MDC for logging
        MDC.put(MDC_CORRELATION_ID_KEY, correlationId);

        log.info("Gateway received request with correlation ID: {}", correlationId);

        // Add to request headers (passes to downstream services)
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(CORRELATION_ID_HEADER, correlationId)
                        .build())
                .build();

        return chain.filter(mutatedExchange)
                .doFinally(signal -> {
                    // Clean up MDC
                    MDC.remove(MDC_CORRELATION_ID_KEY);
                    log.info("Gateway request completed with correlation ID: {}", correlationId);
                });
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public int getOrder() {
        // Execute first (before all other filters)
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

### 2. Microservice Interceptor

```java
// Accounts/src/main/java/com/medo/accounts/interceptor/CorrelationInterceptor.java
package com.medo.accounts.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Correlation ID Interceptor
 *
 * PURPOSE: Extract correlation ID from incoming requests and add to logging context
 */
@Component
public class CorrelationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CorrelationInterceptor.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public boolean preHandle(HttpServletRequest request,
                         HttpServletResponse response,
                         Object handler) {

        // Extract correlation ID from header
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = generateCorrelationId();
            log.warn("No correlation ID received, generated: {}", correlationId);
        }

        // Add to MDC for logging
        MDC.put(MDC_CORRELATION_ID_KEY, correlationId);

        log.info("Accounts service received request with correlation ID: {}", correlationId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler,
                              Exception ex) {
        // Clean up MDC
        String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);
        log.info("Accounts service completed request with correlation ID: {}", correlationId);
        MDC.remove(MDC_CORRELATION_ID_KEY);
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

### 3. WebMvc Configuration

```java
// Accounts/src/main/java/com/medo/accounts/config/WebMvcConfig.java
package com.medo.accounts.config;

import com.medo.accounts.interceptor.CorrelationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CorrelationInterceptor correlationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(correlationInterceptor)
                .addPathPatterns("/**");
    }
}
```

### 4. Logging Configuration

```yaml
# application.yml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n"
```

### 5. Web Client with Correlation ID

```java
// When calling other services, pass the correlation ID
@Component
public class ServiceClient {

    private final WebClient webClient;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    public Mono<ResponseEntity<String>> callOtherService(String correlationId) {
        return webClient.get()
                .uri("http://loans-service/api/v1/loans/fetch")
                .header("X-Correlation-ID", correlationId)  // Pass correlation ID
                .retrieve()
                .toEntity(String.class);
    }

    public Mono<ResponseEntity<String>> callOtherServiceWithMDC() {
        // Get from MDC and pass
        String correlationId = MDC.get("correlationId");
        return callOtherService(correlationId);
    }
}
```

## Log Output Example

```
Gateway Logs:
2024-02-18 14:30:15.123 [http-nio-9090-exec-1] [abc123def456789] INFO  CorrelationIdFilter - Gateway received request with correlation ID: abc123def456789
2024-02-18 14:30:16.456 [http-nio-9090-exec-1] [abc123def456789] INFO  LoggingFilter - POST /api/v1/accounts/create
2024-02-18 14:30:17.789 [http-nio-9090-exec-1] [abc123def456789] INFO  CorrelationIdFilter - Gateway request completed with correlation ID: abc123def456789

Accounts Service Logs:
2024-02-18 14:30:16.500 [http-nio-8080-exec-1] [abc123def456789] INFO  CorrelationInterceptor - Accounts service received request with correlation ID: abc123def456789
2024-02-18 14:30:16.520 [http-nio-8080-exec-1] [abc123def456789] INFO  AccountsController - Creating account for mobile: 1234567890
2024-02-18 14:30:16.580 [http-nio-8080-exec-1] [abc123def456789] INFO  AccountServiceImpl - Customer saved successfully
2024-02-18 14:30:17.700 [http-nio-8080-exec-1] [abc123def456789] INFO  CorrelationInterceptor - Accounts service completed request with correlation ID: abc123def456789

Database Logs:
2024-02-18 14:30:16.600 [mysql-connector] [abc123def456789] INFO  Query: INSERT INTO customers ...
```

## Usage in Your Project

### Search Logs
```bash
# grep for specific correlation ID across all logs
grep "abc123def456789" /var/log/gateway.log
grep "abc123def456789" /var/log/accounts.log
grep "abc123def456789" /var/log/database.log
```

### ELK/Kibana Query
```json
{
  "query": {
    "term": {
      "correlationId": "abc123def456789"
    }
  }
}
```
