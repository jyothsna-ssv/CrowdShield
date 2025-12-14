package com.crowdshield.api.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class HomeController {

    // Redirects root URL to main index page
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "redirect:/admin.html";
    }

    @GetMapping("/api/info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiInfo() {
        return ResponseEntity.ok(Map.of(
                "service", "CrowdShield - Real-Time Content Moderation API",
                "version", "1.0.0",
                "status", "operational",
                "endpoints", Map.of(
                        "health", "/actuator/health",
                        "content", "/api/content",
                        "admin", "/api/admin",
                        "rules", "/api/rules"
                ),
                "documentation", "See README.md for complete API documentation",
                "admin_route", "/admin"
        ));
    }

    // Serves favicon explicitly
    @GetMapping("/favicon.png")
    @ResponseBody
    public ResponseEntity<Resource> favicon() {
        Resource resource = new ClassPathResource("static/favicon.png");
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(resource);
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiEndpoints() {
        return ResponseEntity.ok(Map.of(
                "message", "CrowdShield API",
                "endpoints", Map.of(
                        "content", Map.of(
                                "submit_text", "POST /api/content/text",
                                "submit_image", "POST /api/content/image",
                                "get_status", "GET /api/content/{id}"
                        ),
                        "admin", Map.of(
                                "flagged", "GET /api/admin/flagged",
                                "override", "POST /api/admin/action",
                                "history", "GET /api/admin/history/{id}"
                        ),
                        "rules", Map.of(
                                "get", "GET /api/rules",
                                "update", "POST /api/rules"
                        )
                )
        ));
    }
}

