package com.gateway.walletcentral.modules.refund.controller;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.response.ApiResponse;
import com.gateway.walletcentral.modules.refund.dto.*;
import com.gateway.walletcentral.modules.refund.model.RefundRequestStatus;
import com.gateway.walletcentral.modules.refund.service.RefundRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/refund-requests")
@Tag(name = "RefundRequest", description = "Refund request management and approval operations")
public class RefundController {

    private final RefundRequestService refundRequestService;

    public RefundController(RefundRequestService refundRequestService) {
        this.refundRequestService = refundRequestService;
    }

    @PostMapping
    @Operation(summary = "Create a new refund request for a CHARGE transaction")
    public ResponseEntity<ApiResponse<RefundResponse>> createRefundRequest(
            @Valid @RequestBody RefundCreateRequest request) {
        RefundResponse response = refundRequestService.createRefundRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Refund request created successfully"));
    }

    @GetMapping
    @Operation(summary = "List refund requests with cursor pagination")
    public ResponseEntity<ApiResponse<CursorPage<RefundResponse>>> list(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) RefundRequestStatus status,
            @ModelAttribute CursorParams params) {
        CursorPage<RefundResponse> response = refundRequestService.list(tenantId, status, params);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get refund request by ID")
    public ResponseEntity<ApiResponse<RefundResponse>> getById(@PathVariable UUID id) {
        RefundResponse response = refundRequestService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending refund request - processes REFUND transaction and credits wallet")
    public ResponseEntity<ApiResponse<RefundResponse>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody RefundApproveRequest request) {
        RefundResponse response = refundRequestService.approveRefundRequest(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Refund request approved successfully"));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending refund request")
    public ResponseEntity<ApiResponse<RefundResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRejectRequest request) {
        RefundResponse response = refundRequestService.rejectRefundRequest(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Refund request rejected"));
    }

    @GetMapping("/pending")
    @Operation(summary = "List all pending refund requests")
    public ResponseEntity<ApiResponse<CursorPage<RefundResponse>>> listPending(
            @ModelAttribute CursorParams params) {
        CursorPage<RefundResponse> response = refundRequestService.listPending(params);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
