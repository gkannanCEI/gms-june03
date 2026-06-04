package com.gms.security;

import java.util.List;

public record UserPrincipal(
    String userId,
    List<String> roles,
    Long organizationId
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
