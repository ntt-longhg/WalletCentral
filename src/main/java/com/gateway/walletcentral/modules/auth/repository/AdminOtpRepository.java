package com.gateway.walletcentral.modules.auth.repository;

import com.gateway.walletcentral.modules.auth.model.AdminOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminOtpRepository extends JpaRepository<AdminOtp, UUID> {

    Optional<AdminOtp> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("UPDATE AdminOtp o SET o.used = true WHERE o.email = :email AND o.used = false")
    int markAllUsedByEmail(String email);

    @Modifying
    @Query("DELETE FROM AdminOtp o WHERE o.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}
