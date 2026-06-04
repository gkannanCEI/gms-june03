package com.gms.security;

import com.gms.entity.GmsUser;
import com.gms.repository.GmsUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserDetailsService backed by the gms_user table.
 * Only active when gms.security.auth-mode=local.
 */
@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final GmsUserRepository userRepository;

    public LocalUserDetailsService(GmsUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        GmsUser gmsUser = userRepository.findByUsernameAndActiveTrue(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = Arrays.stream(gmsUser.getRoles().split(","))
            .map(String::trim)
            .filter(r -> !r.isEmpty())
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
            .collect(Collectors.toList());

        return User.builder()
            .username(gmsUser.getUsername())
            .password(gmsUser.getPasswordHash())
            .authorities(authorities)
            .build();
    }
}
