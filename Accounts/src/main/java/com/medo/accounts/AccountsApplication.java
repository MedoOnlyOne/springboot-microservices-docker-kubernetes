package com.medo.accounts;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Accounts Microservice Application
 *
 * PURPOSE: Main entry point for the Accounts microservice
 *
 * Annotations explained:
 * - @SpringBootApplication: Combines @Configuration, @EnableAutoConfiguration, @ComponentScan
 * - @EnableJpaAuditing: Enables automatic tracking of created_at, updated_by, etc.
 * - @EnableFeignClients: Enables declarative HTTP clients for inter-service communication
 * - @OpenAPIDefinition: Configures Swagger/OpenAPI documentation
 *
 * NOTE: No authentication required - all endpoints are public for demo purposes
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "AuditAwareImpl")
@EnableFeignClients
@OpenAPIDefinition(
        info = @Info(
                title = "Accounts Microservice API",
                description = "REST APIs for managing customer accounts in Medo Bank",
                version = "v1.0",
                contact = @io.swagger.v3.oas.annotations.info.Contact(
                        name = "Medo Team",
                        email = "support@medo.com"
                ),
                license = @io.swagger.v3.oas.annotations.info.License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ))
public class AccountsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountsApplication.class, args);
    }

}
