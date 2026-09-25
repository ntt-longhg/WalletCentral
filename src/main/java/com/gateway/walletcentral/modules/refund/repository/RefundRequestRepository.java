package com.gateway.walletcentral.modules.refund.repository;

import com.gateway.walletcentral.modules.refund.model.RefundRequest;
import com.gateway.walletcentral.modules.refund.model.RefundRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    @Query("SELECT rr FROM RefundRequest rr JOIN FETCH rr.transaction JOIN FETCH rr.wallet JOIN FETCH rr.tenant WHERE rr.deletedAt IS NULL AND (:cursor IS NULL OR rr.id > :cursor) AND (:tenantId IS NULL OR rr.tenant.id = :tenantId) AND (:status IS NULL OR rr.status = :status) ORDER BY rr.id ASC")
    List<RefundRequest> findWithCursor(@Param("cursor") UUID cursor,
                                       @Param("tenantId") UUID tenantId,
                                       @Param("status") RefundRequestStatus status,
                                       org.springframework.data.domain.Pageable pageable);

    @Query("SELECT rr FROM RefundRequest rr JOIN FETCH rr.transaction JOIN FETCH rr.wallet JOIN FETCH rr.tenant WHERE rr.id = :id")
    Optional<RefundRequest> findByIdWithRelations(@Param("id") UUID id);

    @Query("SELECT rr FROM RefundRequest rr JOIN FETCH rr.transaction JOIN FETCH rr.wallet JOIN FETCH rr.tenant WHERE rr.status = :status AND rr.deletedAt IS NULL ORDER BY rr.createdAt ASC")
    List<RefundRequest> findByStatusOrderByCreatedAtAsc(@Param("status") RefundRequestStatus status);

    @Query("SELECT rr FROM RefundRequest rr JOIN FETCH rr.transaction JOIN FETCH rr.wallet JOIN FETCH rr.tenant WHERE rr.tenant.id = :tenantId AND rr.status = :status AND rr.deletedAt IS NULL ORDER BY rr.createdAt ASC")
    List<RefundRequest> findByTenantIdAndStatusOrderByCreatedAtAsc(@Param("tenantId") UUID tenantId,
                                                                   @Param("status") RefundRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rr FROM RefundRequest rr WHERE rr.id = :id")
    Optional<RefundRequest> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT CASE WHEN COUNT(rr) > 0 THEN true ELSE false END FROM RefundRequest rr WHERE rr.transaction.id = :transactionId AND rr.status IN (:statuses) AND rr.deletedAt IS NULL")
    boolean existsByTransactionIdAndStatusIn(@Param("transactionId") UUID transactionId,
                                             @Param("statuses") List<RefundRequestStatus> statuses);

    /**
     * Reference IDs of the original transactions that were refunded (APPROVED).
     * Used to exclude refunded usage from invoices.
     */
    @Query("SELECT t.referenceId FROM RefundRequest rr JOIN rr.transaction t WHERE rr.tenant.id = :tenantId AND rr.status = :status AND rr.deletedAt IS NULL")
    List<String> findTransactionReferenceIdsByTenantAndStatus(@Param("tenantId") UUID tenantId,
                                                              @Param("status") RefundRequestStatus status);

    long countByStatusAndDeletedAtIsNull(RefundRequestStatus status);
}
