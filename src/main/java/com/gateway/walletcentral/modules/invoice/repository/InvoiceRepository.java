package com.gateway.walletcentral.modules.invoice.repository;

import com.gateway.walletcentral.modules.invoice.model.Invoice;
import com.gateway.walletcentral.modules.invoice.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    boolean existsByTenantIdAndBillingPeriod(UUID tenantId, String billingPeriod);

    Optional<Invoice> findByTenantIdAndBillingPeriod(UUID tenantId, String billingPeriod);

    @Query("SELECT i FROM Invoice i JOIN FETCH i.tenant JOIN FETCH i.wallet WHERE (:cursor IS NULL OR i.id > :cursor) AND (:tenantId IS NULL OR i.tenant.id = :tenantId) AND (:status IS NULL OR i.status = :status) ORDER BY i.id ASC")
    List<Invoice> findWithCursor(@Param("cursor") UUID cursor,
                                 @Param("tenantId") UUID tenantId,
                                 @Param("status") InvoiceStatus status,
                                 org.springframework.data.domain.Pageable pageable);

    @Query("SELECT i FROM Invoice i JOIN FETCH i.tenant JOIN FETCH i.wallet WHERE i.createdAt >= :startOfMonth AND i.createdAt < :startOfNextMonth")
    List<Invoice> findCurrentMonthInvoices(@Param("startOfMonth") java.time.LocalDateTime startOfMonth,
                                           @Param("startOfNextMonth") java.time.LocalDateTime startOfNextMonth);
}
