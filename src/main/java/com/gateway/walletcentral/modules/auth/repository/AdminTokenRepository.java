package com.gateway.walletcentral.modules.auth.repository;

import com.gateway.walletcentral.modules.auth.model.AdminToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminTokenRepository extends JpaRepository<AdminToken, UUID> {

    Optional<AdminToken> findByToken(String token);

    boolean existsByTokenAndExpiresAtAfter(String token, LocalDateTime now);

    int deleteByToken(String token);

    int deleteByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("DELETE FROM AdminToken t WHERE t.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}
