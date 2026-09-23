package com.gateway.walletcentral.modules.wallet.repository;

import com.gateway.walletcentral.modules.wallet.model.Wallet;
import com.gateway.walletcentral.modules.wallet.model.WalletStatus;
import com.gateway.walletcentral.modules.wallet.model.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByTenantId(UUID tenantId);

    boolean existsByTenantId(UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.tenant.id = :tenantId")
    Optional<Wallet> findByTenantIdForUpdate(@Param("tenantId") UUID tenantId);

    @Query("SELECT w FROM Wallet w JOIN FETCH w.tenant WHERE w.deletedAt IS NULL AND (:cursor IS NULL OR w.id > :cursor) AND (:tenantId IS NULL OR w.tenant.id = :tenantId) AND (:type IS NULL OR w.type = :type) AND (:status IS NULL OR w.status = :status) ORDER BY w.id ASC")
    List<Wallet> findWithCursor(@Param("cursor") UUID cursor,
            @Param("tenantId") UUID tenantId,
            @Param("type") WalletType type,
            @Param("status") WalletStatus status,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT w.tenant.id FROM Wallet w WHERE w.type = :type AND w.deletedAt IS NULL")
    List<UUID> findTenantIdsByType(@Param("type") WalletType type);

    @Query("SELECT w FROM Wallet w JOIN FETCH w.tenant WHERE w.id = :id")
    Optional<Wallet> findByIdWithTenant(@Param("id") UUID id);

}
