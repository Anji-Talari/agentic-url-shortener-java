package com.example.agentic;

import java.net.URI;
import java.util.Map;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class UrlController {
    private final UrlService urls;
    private final AdminGuard admin;

    public UrlController(UrlService urls, AdminGuard admin) {
        this.urls = urls;
        this.admin = admin;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<Map<String, Object>> create(@RequestBody UrlService.CreateUrl request) {
        return ResponseEntity.status(201).body(urls.create(request));
    }

    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(urls.resolve(code))).build();
    }

    @GetMapping("/api/urls/{code}/analytics")
    public Map<String, Object> stats(@PathVariable String code, @RequestHeader("X-Admin-Key") String key) {
        admin.check(key);
        return urls.stats(code);
    }

    @DeleteMapping("/api/urls/{code}")
    public ResponseEntity<Void> disable(@PathVariable String code, @RequestHeader("X-Admin-Key") String key) {
        admin.check(key);
        urls.disable(code);
        return ResponseEntity.noContent().build();
    }
}
