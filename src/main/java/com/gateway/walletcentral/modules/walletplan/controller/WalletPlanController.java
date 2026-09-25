package com.gateway.walletcentral.modules.walletplan.controller;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.response.ApiResponse;
import com.gateway.walletcentral.modules.walletplan.dto.*;
import com.gateway.walletcentral.modules.walletplan.model.WalletPlanStatus;
import com.gateway.walletcentral.modules.walletplan.service.WalletPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet-plans")
@Tag(name = "WalletPlan", description = "Wallet plan management and approval operations")
public class WalletPlanController {

    private final WalletPlanService walletPlanService;

    public WalletPlanController(WalletPlanService walletPlanService) {
        this.walletPlanService = walletPlanService;
    }

    @PostMapping
    @Operation(summary = "Create a new wallet plan (topup request)")
    public ResponseEntity<ApiResponse<WalletPlanResponse>> create(@Valid @RequestBody WalletPlanCreateRequest request) {
        WalletPlanResponse response = walletPlanService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Wallet plan created successfully"));
    }

    @GetMapping
    @Operation(summary = "List wallet plans with cursor pagination")
    public ResponseEntity<ApiResponse<CursorPage<WalletPlanResponse>>> list(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) WalletPlanStatus status,
            @ModelAttribute CursorParams params) {
        CursorPage<WalletPlanResponse> response = walletPlanService.list(tenantId, status, params);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get wallet plan by ID")
    public ResponseEntity<ApiResponse<WalletPlanResponse>> getById(@PathVariable UUID id) {
        WalletPlanResponse response = walletPlanService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending wallet plan")
    public ResponseEntity<ApiResponse<WalletPlanResponse>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody WalletPlanApproveRequest request) {
        WalletPlanResponse response = walletPlanService.approve(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Wallet plan approved successfully"));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending wallet plan")
    public ResponseEntity<ApiResponse<WalletPlanResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody WalletPlanRejectRequest request) {
        WalletPlanResponse response = walletPlanService.reject(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Wallet plan rejected"));
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending wallet plans")
    public ResponseEntity<ApiResponse<CursorPage<WalletPlanResponse>>> listPending(
            @ModelAttribute CursorParams params) {
        CursorPage<WalletPlanResponse> response = walletPlanService.listPending(params);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
