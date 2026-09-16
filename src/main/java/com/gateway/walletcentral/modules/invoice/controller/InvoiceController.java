package com.gateway.walletcentral.modules.invoice.controller;

import com.gateway.walletcentral.core.annotation.RequirePermission;
import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.response.ApiResponse;
import com.gateway.walletcentral.modules.invoice.dto.*;
import com.gateway.walletcentral.modules.invoice.model.InvoiceStatus;
import com.gateway.walletcentral.modules.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoice", description = "Invoice management operations")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @RequirePermission("INVOICE_CREATE")
    @Operation(summary = "Create a new invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(@Valid @RequestBody InvoiceCreateRequest request) {
        InvoiceResponse response = invoiceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Invoice created successfully"));
    }

    @GetMapping
    @RequirePermission("INVOICE_VIEW")
    @Operation(summary = "List invoices with cursor pagination")
    public ResponseEntity<ApiResponse<CursorPage<InvoiceResponse>>> list(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) InvoiceStatus status,
            @ModelAttribute CursorParams params) {
        CursorPage<InvoiceResponse> response = invoiceService.list(tenantId, status, params);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @RequirePermission("INVOICE_VIEW")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/pay")
    @RequirePermission("INVOICE_PAY")
    @Operation(summary = "Mark invoice as paid")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markAsPaid(
            @PathVariable UUID id,
            @Valid @RequestBody InvoicePayRequest request) {
        InvoiceResponse response = invoiceService.markAsPaid(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Invoice marked as paid"));
    }

    @PostMapping("/generate")
    @RequirePermission("INVOICE_GENERATE")
    @Operation(summary = "Generate invoice from usage logs (manual trigger)")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generate(
            @Valid @RequestBody InvoiceGenerateRequest request) {
        InvoiceResponse response = invoiceService.generateInvoice(
                request.getTenantId(),
                request.getEffectiveBillingPeriod(),
                request.getUpdatedBy()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Invoice generated successfully"));
    }

    @PostMapping("/generate-all")
    @RequirePermission("INVOICE_GENERATE")
    @Operation(summary = "Generate invoices for all tenants for a billing period")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateAll(
            @RequestParam(required = false) String billingPeriod,
            @RequestParam(defaultValue = "admin") String updatedBy) {
        String period = billingPeriod != null ? billingPeriod :
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

        // Validate: reject future months
        java.time.YearMonth selected = java.time.YearMonth.parse(period);
        java.time.YearMonth current = java.time.YearMonth.now();
        if (selected.isAfter(current)) {
            throw new com.gateway.walletcentral.core.exception.BusinessException(
                    "INVALID_BILLING_PERIOD", "Không thể tạo hóa đơn cho tháng tương lai");
        }

        int count = invoiceService.generateAllInvoicesForPeriod(period, updatedBy);
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("generatedCount", count, "billingPeriod", period),
                "Invoices generated successfully"
        ));
    }
}
