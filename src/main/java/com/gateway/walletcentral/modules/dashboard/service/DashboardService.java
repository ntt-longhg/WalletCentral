package com.gateway.walletcentral.modules.dashboard.service;

import com.gateway.walletcentral.modules.dashboard.dto.DashboardSummaryResponse;
import com.gateway.walletcentral.modules.refund.model.RefundRequestStatus;
import com.gateway.walletcentral.modules.refund.repository.RefundRequestRepository;
import com.gateway.walletcentral.modules.servicecatalog.repository.ServiceRepository;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import com.gateway.walletcentral.modules.transaction.repository.TransactionRepository;
import com.gateway.walletcentral.modules.wallet.repository.WalletRepository;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlanStatus;
import com.gateway.walletcentral.modules.walletplan.repository.WalletPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final TenantRepository tenantRepository;
    private final WalletRepository walletRepository;
    private final ServiceRepository serviceRepository;
    private final TransactionRepository transactionRepository;
    private final WalletPlanRepository walletPlanRepository;
    private final RefundRequestRepository refundRequestRepository;

    public DashboardService(TenantRepository tenantRepository,
            WalletRepository walletRepository,
            ServiceRepository serviceRepository,
            TransactionRepository transactionRepository,
            WalletPlanRepository walletPlanRepository,
            RefundRequestRepository refundRequestRepository) {
        this.tenantRepository = tenantRepository;
        this.walletRepository = walletRepository;
        this.serviceRepository = serviceRepository;
        this.transactionRepository = transactionRepository;
        this.walletPlanRepository = walletPlanRepository;
        this.refundRequestRepository = refundRequestRepository;
    }

    /**
     * Counters only (COUNT queries). Never reuse paginated list queries here:
     * page sizes would make the numbers wrong.
     */
    public DashboardSummaryResponse getSummary() {
        return DashboardSummaryResponse.builder()
                .tenantCount(tenantRepository.count())
                .walletCount(walletRepository.count())
                .serviceCount(serviceRepository.count())
                .transactionCount(transactionRepository.count())
                .pendingPlanCount(walletPlanRepository.countByStatusAndDeletedAtIsNull(WalletPlanStatus.PENDING))
                .pendingRefundCount(
                        refundRequestRepository.countByStatusAndDeletedAtIsNull(RefundRequestStatus.PENDING))
                .build();
    }
}
