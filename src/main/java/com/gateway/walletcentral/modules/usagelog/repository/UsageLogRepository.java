package com.gateway.walletcentral.modules.usagelog.repository;

import com.gateway.walletcentral.modules.usagelog.model.UsageLog;
import com.gateway.walletcentral.modules.wallet.model.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, UUID> {

        @Query("SELECT ul FROM UsageLog ul LEFT JOIN FETCH ul.tenant LEFT JOIN FETCH ul.service WHERE (:cursor IS NULL OR ul.id > :cursor) AND (:tenantId IS NULL OR ul.tenant.id = :tenantId) AND (:serviceId IS NULL OR ul.service.id = :serviceId) ORDER BY ul.id ASC")
        List<UsageLog> findWithCursor(@Param("cursor") UUID cursor,
                        @Param("tenantId") UUID tenantId,
                        @Param("serviceId") UUID serviceId,
                        org.springframework.data.domain.Pageable pageable);

        /**
         * Sum total_charged for POSTPAID usage logs within a billing period.
         * Only POSTPAID usage is invoiced (PREPAID is deducted from balance in
         * real-time).
         */
        @Query("SELECT COALESCE(SUM(ul.totalCharged), 0) FROM UsageLog ul WHERE ul.tenant.id = :tenantId AND ul.walletTypeSnapshot = :walletType AND ul.createdAt >= :startOfMonth AND ul.createdAt <= :endOfMonth")
        BigDecimal sumChargedByTenantAndPeriod(@Param("tenantId") UUID tenantId,
                        @Param("walletType") WalletType walletType,
                        @Param("startOfMonth") LocalDateTime startOfMonth,
                        @Param("endOfMonth") LocalDateTime endOfMonth);

        /**
         * Same as above but excludes refunded usage (by reference ID).
         * Only call when the exclusion list is non-empty (empty NOT IN is invalid JPQL).
         */
        @Query("SELECT COALESCE(SUM(ul.totalCharged), 0) FROM UsageLog ul WHERE ul.tenant.id = :tenantId AND ul.walletTypeSnapshot = :walletType AND ul.createdAt >= :startOfMonth AND ul.createdAt <= :endOfMonth AND ul.referenceId NOT IN :excludedReferenceIds")
        BigDecimal sumChargedByTenantAndPeriodExcluding(@Param("tenantId") UUID tenantId,
                        @Param("walletType") WalletType walletType,
                        @Param("startOfMonth") LocalDateTime startOfMonth,
                        @Param("endOfMonth") LocalDateTime endOfMonth,
                        @Param("excludedReferenceIds") List<String> excludedReferenceIds);

        /**
         * Usage logs comprising an invoice (POSTPAID in period), with service fetched.
         * UUID v7 IDs are time-ordered, so id ASC matches chronological order
         * and supports cursor pagination like other list queries.
         */
        @Query("SELECT ul FROM UsageLog ul LEFT JOIN FETCH ul.service WHERE ul.tenant.id = :tenantId AND ul.walletTypeSnapshot = :walletType AND ul.createdAt >= :startOfMonth AND ul.createdAt <= :endOfMonth AND (:cursor IS NULL OR ul.id > :cursor) ORDER BY ul.id ASC")
        List<UsageLog> findByTenantAndPeriod(@Param("tenantId") UUID tenantId,
                        @Param("walletType") WalletType walletType,
                        @Param("startOfMonth") LocalDateTime startOfMonth,
                        @Param("endOfMonth") LocalDateTime endOfMonth,
                        @Param("cursor") UUID cursor,
                        org.springframework.data.domain.Pageable pageable);

        boolean existsByReferenceId(String referenceId);
}
