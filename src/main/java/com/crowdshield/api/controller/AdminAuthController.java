package com.crowdshield.api.controller;

import com.crowdshield.util.ErrorUtils;
import com.crowdshield.util.JwtUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    // Authenticates admin user and returns JWT token on success
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (adminUsername.equals(request.getUsername()) && adminPassword.equals(request.getPassword())) {
            String token = jwtUtil.generateToken(request.getUsername());
            log.info("Admin logged in: {}", request.getUsername());
            return ResponseEntity.ok(new LoginResponse(true, "Login successful", token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorUtils.createErrorResponse("INVALID_CREDENTIALS", "Invalid username or password"));
    }

    // Checks if the provided JWT token is valid and returns authentication status
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("authenticated", true);
                response.put("message", "Authenticated");
                response.put("token", token);
                response.put("username", username);
                return ResponseEntity.ok(response);
            }
        }
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("authenticated", false);
        response.put("message", "Not authenticated");
        return ResponseEntity.ok(response);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private boolean authenticated;
        private String message;
        private String token;

        public LoginResponse(boolean authenticated, String message, String token) {
            this.authenticated = authenticated;
            this.message = message;
            this.token = token;
        }
    }
}

