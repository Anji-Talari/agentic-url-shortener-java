package com.example.agentic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminGuard {
    private final String key;

    public AdminGuard(@Value("${app.admin-key}") String key) {
        this.key = key;
    }

    public void check(String provided) {
        if (provided == null || !provided.equals(key))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid X-Admin-Key");
    }
}
