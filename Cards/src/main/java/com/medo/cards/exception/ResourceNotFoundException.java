package com.medo.cards.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String key, String value) {
        super(String.format("%s is not found for %s: %s", resource, key, value));
    }
}
