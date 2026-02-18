package com.medo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Application
 *
 * PURPOSE: Acts as a single entry point for all microservices
 *
 * KEY FEATURES:
 * 1. ROUTING: Routes requests to appropriate microservices based on URL patterns
 * 2. LOAD BALANCING: Distributes traffic across multiple service instances
 * 3. SERVICE DISCOVERY: Discovers services via Eureka
 * 4. CIRCUIT BREAKER: Prevents cascading failures with Resilience4j
 * 5. RATE LIMITING: Controls request rate per client
 * 6. CROSS-ORIGIN: Handles CORS for frontend integration
 * 7. LOGGING: Centralized request/response logging
 *
 * HOW IT WORKS:
 * 1. Client requests hit the Gateway first
 * 2. Gateway matches routes based on URL patterns
 * 3. Forwards request to discovered service via Eureka
 * 4. Applies filters (logging, rate limiting, authentication)
 * 5. Returns response to client
 *
 * TARGET: Provides a unified API endpoint, improves security, and manages cross-cutting concerns
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}
