package com.medo.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server Application
 *
 * PURPOSE: Provides Service Discovery capabilities for microservices
 *
 * How it works:
 * 1. Microservices register themselves with Eureka Server on startup
 * 2. Eureka maintains a registry of all available service instances
 * 3. Services can discover each other by name instead of hardcoded URLs
 * 4. Eureka provides health checks and removes unhealthy instances
 *
 * Benefits:
 * - Dynamic service discovery without hardcoded URLs
 * - Load balancing through client-side discovery
 * - Automatic failover to healthy instances
 * - Self-healing architecture
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }

}
