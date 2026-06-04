package com.gms.repository;

import com.gms.entity.GmsUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GmsUserRepository extends JpaRepository<GmsUser, Long> {
    Optional<GmsUser> findByUsernameAndActiveTrue(String username);
}
