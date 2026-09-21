package com.gateway.walletcentral.modules.transaction.repository;

import com.gateway.walletcentral.modules.transaction.model.Transaction;
import com.gateway.walletcentral.modules.transaction.model.TransactionStatus;
import com.gateway.walletcentral.modules.transaction.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t JOIN FETCH t.wallet WHERE (:cursor IS NULL OR t.id > :cursor) AND (:walletId IS NULL OR t.wallet.id = :walletId) AND (:type IS NULL OR t.type = :type) AND (:status IS NULL OR t.status = :status) ORDER BY t.id ASC")
    List<Transaction> findWithCursor(@Param("cursor") UUID cursor,
                                     @Param("walletId") UUID walletId,
                                     @Param("type") TransactionType type,
                                     @Param("status") TransactionStatus status,
                                     org.springframework.data.domain.Pageable pageable);

    List<Transaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    boolean existsByReferenceId(String referenceId);
}
