package com.chatapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    @GetMapping("/auth-info")
    public ResponseEntity<Map<String, Object>> getAuthInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> info = new HashMap<>();

        if (auth != null) {
            info.put("authenticated", auth.isAuthenticated());
            info.put("principal", auth.getPrincipal().toString());
            info.put("authorities", auth.getAuthorities().toString());
            info.put("name", auth.getName());
        } else {
            info.put("authenticated", false);
            info.put("message", "No authentication found");
        }

        log.info("Debug: Auth info requested - {}", info);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/test-user-role")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> testUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> info = new HashMap<>();

        info.put("message", "USER role test passed!");
        info.put("user", auth.getName());
        info.put("authorities", auth.getAuthorities().toString());

        log.info("Debug: USER role test passed for {}", auth.getName());
        return ResponseEntity.ok(info);
    }

    @GetMapping("/test-admin-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> info = new HashMap<>();

        info.put("message", "ADMIN role test passed!");
        info.put("user", auth.getName());
        info.put("authorities", auth.getAuthorities().toString());

        log.info("Debug: ADMIN role test passed for {}", auth.getName());
        return ResponseEntity.ok(info);
    }

    @GetMapping("/file-upload-info")
    public ResponseEntity<Map<String, Object>> getFileUploadInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("message", "File upload system information");
        info.put("uploadEndpoint", "/api/files/upload");
        info.put("supportedTypes", "image/jpeg, image/png, image/gif, application/pdf, text/plain");
        info.put("maxFileSize", "1GB");
        info.put("instructions", "Upload files via REST API first, then send file URL via WebSocket");
        info.put("correctFlow", "1. POST /api/files/upload -> 2. Get file URL -> 3. Send URL via WebSocket");

        return ResponseEntity.ok(info);
    }
}
