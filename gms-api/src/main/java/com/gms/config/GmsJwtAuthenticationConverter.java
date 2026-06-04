package com.gms.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Converts the Azure AD B2C JWT "roles" claim into Spring Security authorities.
 * 
 * The spec defines that the JWT carries a "roles" claim (string array) with values
 * like "ADMIN" and "APPLICANT". This converter maps them to Spring Security authorities
 * with the ROLE_ prefix (e.g., ROLE_ADMIN, ROLE_APPLICANT) which is the standard
 * convention for hasAuthority() checks in SecurityConfig.
 */
public class GmsJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            for (String role : roles) {
                // Map "ADMIN" -> "ROLE_ADMIN", "APPLICANT" -> "ROLE_APPLICANT"
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        return authorities;
    }
}
