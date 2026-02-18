package com.medo.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Handler
 *
 * PURPOSE: Provides fallback responses when circuit breaker trips
 *
 * HOW IT WORKS:
 * 1. Called when downstream service is unavailable
 * 2. Returns a friendly error message
 * 3. Logs the failure for monitoring
 *
 * TARGET: Prevents cascading failures and provides graceful degradation
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackHandler {

    @RequestMapping("/accounts")
    public ResponseEntity<Map<String, Object>> accountsFallback() {
        log.error("Accounts Service is unavailable. Circuit breaker activated.");

        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("status", 503);
        fallbackResponse.put("error", "Service Unavailable");
        fallbackResponse.put("message", "Accounts service is currently unavailable. Please try again later.");
        fallbackResponse.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }
}
