package com.gateway.walletcentral.config;

import com.gateway.walletcentral.core.config.AppConfigCache;
import com.gateway.walletcentral.core.event.ConfigReloadedEvent;
import com.gateway.walletcentral.modules.auth.service.AuthService;
import com.gateway.walletcentral.modules.invoice.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;

/**
 * Cron schedules driven by system_config (SCHEDULER group).
 * <p>
 * - Registered once the app is ready; re-registered whenever configs are
 * reloaded (POST /api/v1/system-configs/reload), so cron changes apply
 * without restart.
 * - Invalid cron expressions are rejected with an error log and the previous
 * schedule is kept (never crash the app because of a bad UI edit).
 */
@Component
public class DynamicScheduler {

    private static final Logger log = LoggerFactory.getLogger(DynamicScheduler.class);
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final String DEFAULT_CLEANUP_CRON = "0 */30 * * * *";
    private static final String DEFAULT_INVOICE_CRON = "0 0 2 1 * ?";

    private final TaskScheduler taskScheduler;
    private final AppConfigCache configCache;
    private final AuthService authService;
    private final InvoiceService invoiceService;

    private ScheduledFuture<?> cleanupFuture;
    private ScheduledFuture<?> invoiceFuture;
    private String activeCleanupCron;
    private String activeInvoiceCron;

    public DynamicScheduler(TaskScheduler taskScheduler,
            AppConfigCache configCache,
            AuthService authService,
            InvoiceService invoiceService) {
        this.taskScheduler = taskScheduler;
        this.configCache = configCache;
        this.authService = authService;
        this.invoiceService = invoiceService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleOnStartup() {
        log.info("========== DYNAMIC SCHEDULER INIT ==========");
        scheduleAll();
        log.info("========== END DYNAMIC SCHEDULER INIT ==========");
    }

    @EventListener(ConfigReloadedEvent.class)
    public void scheduleOnConfigReload(ConfigReloadedEvent event) {
        log.info("========== DYNAMIC SCHEDULER RESCHEDULE ({} config keys changed) ==========",
                event.getChangedKeys() == null ? 0 : event.getChangedKeys().size());
        scheduleAll();
        log.info("========== END DYNAMIC SCHEDULER RESCHEDULE ==========");
    }

    public synchronized void scheduleAll() {
        scheduleCleanup();
        scheduleInvoices();
    }

    private synchronized void scheduleCleanup() {
        boolean enabled = configCache.getBoolean("scheduler.cleanup_enabled", true);
        String cron = configCache.getValue("scheduler.cleanup_cron", DEFAULT_CLEANUP_CRON).trim();
        if (!enabled) {
            cancelQuietly(cleanupFuture, "auth cleanup");
            cleanupFuture = null;
            activeCleanupCron = null;
            log.info("scheduler: auth cleanup is DISABLED (scheduler.cleanup_enabled=false)");
            return;
        }
        if (!CronExpression.isValidExpression(cron)) {
            log.error("scheduler: invalid cleanup cron '{}', keeping previous schedule '{}'",
                    cron, activeCleanupCron);
            return;
        }
        if (cron.equals(activeCleanupCron) && cleanupFuture != null && !cleanupFuture.isCancelled()) {
            log.info("scheduler: auth cleanup already scheduled with cron '{}', no change", cron);
            return;
        }
        cancelQuietly(cleanupFuture, "auth cleanup");
        cleanupFuture = taskScheduler.schedule(this::cleanupExpiredAuthData, new CronTrigger(cron));
        activeCleanupCron = cron;
        log.info("scheduler: auth cleanup scheduled with cron '{}'", cron);
    }

    private synchronized void scheduleInvoices() {
        boolean enabled = configCache.getBoolean("scheduler.invoice_enabled", true);
        String cron = configCache.getValue("scheduler.invoice_cron", DEFAULT_INVOICE_CRON).trim();
        if (!enabled) {
            cancelQuietly(invoiceFuture, "monthly invoices");
            invoiceFuture = null;
            activeInvoiceCron = null;
            log.info("scheduler: monthly invoices are DISABLED (scheduler.invoice_enabled=false)");
            return;
        }
        if (!CronExpression.isValidExpression(cron)) {
            log.error("scheduler: invalid invoice cron '{}', keeping previous schedule '{}'",
                    cron, activeInvoiceCron);
            return;
        }
        if (cron.equals(activeInvoiceCron) && invoiceFuture != null && !invoiceFuture.isCancelled()) {
            log.info("scheduler: monthly invoices already scheduled with cron '{}', no change", cron);
            return;
        }
        cancelQuietly(invoiceFuture, "monthly invoices");
        invoiceFuture = taskScheduler.schedule(this::generateMonthlyInvoices, new CronTrigger(cron));
        activeInvoiceCron = cron;
        log.info("scheduler: monthly invoices scheduled with cron '{}'", cron);
    }

    /**
     * Cleanup expired OTPs and tokens.
     * Prevents data accumulation from users who close browser without logging out.
     */
    public void cleanupExpiredAuthData() {
        int deletedOtps = authService.cleanupExpiredOtps();
        int deletedTokens = authService.cleanupExpiredTokens();
        if (deletedOtps > 0 || deletedTokens > 0) {
            log.info("Scheduled cleanup: removed {} expired OTPs, {} expired tokens", deletedOtps, deletedTokens);
        }
    }

    /**
     * Auto-generate invoices for the previous month.
     */
    public void generateMonthlyInvoices() {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        String billingPeriod = previousMonth.format(PERIOD_FORMAT);

        log.info("========== INVOICE SCHEDULER START ========== Generating invoices for period: {}", billingPeriod);

        try {
            int count = invoiceService.generateAllInvoicesForPeriod(billingPeriod, "SYSTEM_SCHEDULER");
            log.info("========== INVOICE SCHEDULER END ========== Generated {} invoices for period: {}", count,
                    billingPeriod);
        } catch (Exception e) {
            log.error("========== INVOICE SCHEDULER END ========== FAILED for period: {}", billingPeriod, e);
        }
    }

    private void cancelQuietly(ScheduledFuture<?> future, String name) {
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
            log.info("scheduler: cancelled previous schedule for {}", name);
        }
    }
}
