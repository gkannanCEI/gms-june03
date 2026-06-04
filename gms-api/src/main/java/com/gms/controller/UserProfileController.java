package com.gms.controller;

import com.gms.security.SecurityContextProvider;
import com.gms.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * GAP-11: Provides a lightweight /api/me endpoint that returns the authenticated
 * user's id and roles.  The Angular AuthService uses this instead of the brittle
 * heuristic that inferred roles from HTTP response codes on /api/admin/questions.
 *
 * Contract:
 *   GET /api/me
 *   → 200 { "userId": "...", "roles": ["ADMIN"] }  (authenticated user)
 *   → 401 (unauthenticated — handled by Spring Security before this controller)
 */
@RestController
public class UserProfileController {

    private final SecurityContextProvider securityContextProvider;

    public UserProfileController(SecurityContextProvider securityContextProvider) {
        this.securityContextProvider = securityContextProvider;
    }

    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        UserPrincipal principal = securityContextProvider.getCurrentUser();
        return ResponseEntity.ok(Map.of(
            "userId", principal.userId(),
            "roles", principal.roles(),
            "organizationId", principal.organizationId() != null ? principal.organizationId() : ""
        ));
    }
}
