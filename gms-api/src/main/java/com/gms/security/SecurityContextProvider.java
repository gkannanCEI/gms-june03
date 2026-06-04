package com.gms.security;

import com.gms.entity.GmsUser;
import com.gms.repository.GmsUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unified SecurityContextProvider that works in both auth modes:
 * - b2c: extracts UserPrincipal from JWT claims
 * - local: extracts UserPrincipal from gms_user table via authenticated username
 */
@Component
public class SecurityContextProvider {

    @Value("${gms.security.auth-mode:b2c}")
    private String authMode;

    @Value("${gms.security.role-claim-key:roles}")
    private String roleClaimKey;

    @Value("${gms.security.org-id-claim-key:extension_organizationId}")
    private String orgIdClaimKey;

    private final GmsUserRepository userRepository;

    public SecurityContextProvider(GmsUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
            return new UserPrincipal("anonymous", Collections.emptyList(), null);
        }

        if ("local".equals(authMode)) {
            return extractFromLocalAuth(authentication);
        } else {
            return extractFromJwt(authentication);
        }
    }

    private UserPrincipal extractFromJwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String userId = jwt.getSubject();
            List<String> roles = jwt.getClaimAsStringList(roleClaimKey);
            if (roles == null) roles = Collections.emptyList();
            String orgIdClaim = jwt.getClaimAsString(orgIdClaimKey);
            Long organizationId = null;
            if (orgIdClaim != null) {
                try {
                    organizationId = Long.parseLong(orgIdClaim);
                } catch (NumberFormatException e) {
                    // Non-numeric organizationId claim — log and continue without org context
                }
            }
            return new UserPrincipal(userId, roles, organizationId);
        }
        return new UserPrincipal("anonymous", Collections.emptyList(), null);
    }

    private UserPrincipal extractFromLocalAuth(Authentication authentication) {
        String username = authentication.getName();
        GmsUser user = userRepository.findByUsernameAndActiveTrue(username).orElse(null);
        if (user == null) {
            return new UserPrincipal(username, Collections.emptyList(), null);
        }

        List<String> roles = Arrays.stream(user.getRoles().split(","))
            .map(String::trim)
            .filter(r -> !r.isEmpty())
            .toList();

        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        return new UserPrincipal(String.valueOf(user.getId()), roles, orgId);
    }
}
