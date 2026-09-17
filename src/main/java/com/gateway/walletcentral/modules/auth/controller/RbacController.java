package com.gateway.walletcentral.modules.auth.controller;

import com.gateway.walletcentral.config.SecurityConfig;
import com.gateway.walletcentral.core.annotation.RequirePermission;
import com.gateway.walletcentral.core.cursor.CursorParams;
import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.response.ApiResponse;
import com.gateway.walletcentral.modules.auth.dto.*;
import com.gateway.walletcentral.modules.auth.service.RbacService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rbac")
@Tag(name = "RBAC", description = "Role-Based Access Control management (admin only)")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    // ====================== USERS ======================

    @GetMapping("/users")
    @RequirePermission("RBAC_VIEW")
    @Operation(summary = "List all admin users with roles")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> listUsers(HttpServletRequest request) {
        String email = (String) request.getAttribute(SecurityConfig.REQUEST_ATTR_EMAIL);
        List<AdminUserResponse> response = rbacService.listUsers(email);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/users/{email}/info")
    @RequirePermission("RBAC_VIEW")
    @Operation(summary = "Get user info by email with role and effective permissions")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserInfo(@PathVariable String email) {
        UserInfoResponse response = rbacService.getUserInfoByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ====================== ROLES ======================

    @GetMapping("/roles")
    @RequirePermission("RBAC_VIEW")
    @Operation(summary = "List all roles with cursor pagination")
    public ResponseEntity<ApiResponse<CursorPage<RoleResponse>>> listRoles(
            @ModelAttribute CursorParams params) {
        UUID cursor = params.getCursor() != null ? UUID.fromString(params.getCursor()) : null;
        CursorPage<RoleResponse> response = rbacService.listRoles(cursor, params.getSize());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/roles/{id}")
    @RequirePermission("RBAC_VIEW")
    @Operation(summary = "Get role by ID with permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable UUID id) {
        RoleResponse response = rbacService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/roles")
    @RequirePermission("RBAC_MANAGE_ROLES")
    @Operation(summary = "Create a new role")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody RoleCreateRequest request) {
        RoleResponse response = rbacService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Role created successfully"));
    }

    @PutMapping("/roles/{id}")
    @RequirePermission("RBAC_MANAGE_ROLES")
    @Operation(summary = "Update a role")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleUpdateRequest request) {
        RoleResponse response = rbacService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Role updated successfully"));
    }

    @DeleteMapping("/roles/{id}")
    @RequirePermission("RBAC_MANAGE_ROLES")
    @Operation(summary = "Delete a role (system roles cannot be deleted)")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        rbacService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Role deleted successfully"));
    }

    // ====================== PERMISSIONS ======================

    @GetMapping("/permissions")
    @RequirePermission("RBAC_VIEW")
    @Operation(summary = "List all available permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> listPermissions(
            @RequestParam(required = false) String module) {
        List<PermissionResponse> response = module != null
                ? rbacService.listPermissionsByModule(module)
                : rbacService.listPermissions();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ====================== USER ROLE ASSIGNMENT (by email) ======================

    @PostMapping("/users/{email}/role")
    @RequirePermission("RBAC_MANAGE_ROLES")
    @Operation(summary = "Assign a role to a user by email")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUser(
            @PathVariable String email,
            @Valid @RequestBody UserRoleAssignRequest request) {
        rbacService.assignRoleToUser(email, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Role assigned successfully"));
    }

    @DeleteMapping("/users/{email}/role")
    @RequirePermission("RBAC_MANAGE_ROLES")
    @Operation(summary = "Remove role from a user by email")
    public ResponseEntity<ApiResponse<Void>> removeRoleFromUser(@PathVariable String email) {
        rbacService.removeRoleFromUser(email);
        return ResponseEntity.ok(ApiResponse.ok(null, "Role removed successfully"));
    }

    // ====================== USER PERMISSION OVERRIDES (by email)
    // ======================

    @PostMapping("/users/{email}/permissions")
    @RequirePermission("RBAC_MANAGE_USER_PERMISSIONS")
    @Operation(summary = "Grant a permission override to a user by email")
    public ResponseEntity<ApiResponse<Void>> grantPermissionToUser(
            @PathVariable String email,
            @Valid @RequestBody UserPermissionRequest request) {
        rbacService.grantPermissionToUser(email, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Permission granted"));
    }

    @DeleteMapping("/users/{email}/permissions/{permissionId}")
    @RequirePermission("RBAC_MANAGE_USER_PERMISSIONS")
    @Operation(summary = "Revoke a permission override from a user by email")
    public ResponseEntity<ApiResponse<Void>> revokePermissionFromUser(
            @PathVariable String email,
            @PathVariable UUID permissionId) {
        rbacService.revokePermissionFromUser(email, permissionId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Permission revoked"));
    }
}
