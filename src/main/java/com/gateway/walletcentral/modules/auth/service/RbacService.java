package com.gateway.walletcentral.modules.auth.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.exception.BusinessException;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.auth.dto.*;
import com.gateway.walletcentral.modules.auth.model.*;
import com.gateway.walletcentral.modules.auth.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RbacService {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AdminUserRoleRepository userRoleRepository;
    private final AdminUserPermissionRepository userPermissionRepository;
    private final AdminUserRepository adminUserRepository;

    public RbacService(RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            AdminUserRoleRepository userRoleRepository,
            AdminUserPermissionRepository userPermissionRepository,
            AdminUserRepository adminUserRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.adminUserRepository = adminUserRepository;
    }

    // ====================== PERMISSION RESOLUTION ======================

    /**
     * Resolve all effective permissions for a user by email.
     * Combines role permissions + user-specific overrides.
     */
    @Transactional(readOnly = true)
    public Set<String> getPermissionsByEmail(String email) {
        var adminUser = adminUserRepository.findByEmail(email).orElse(null);
        if (adminUser == null) {
            return Set.of();
        }

        var userRole = userRoleRepository.findByAdminUserId(adminUser.getId().toString()).orElse(null);
        if (userRole == null) {
            return Set.of();
        }

        // Base permissions from role
        Set<String> permissions = userRole.getRole().getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        // Apply user-specific overrides
        List<AdminUserPermission> overrides = userPermissionRepository.findByAdminUserId(adminUser.getId().toString());
        for (AdminUserPermission override : overrides) {
            if (override.getIsGranted()) {
                permissions.add(override.getPermission().getCode());
            } else {
                permissions.remove(override.getPermission().getCode());
            }
        }

        return permissions;
    }

    /**
     * Resolve permissions for a token (looks up email from token, then resolves).
     */
    @Transactional(readOnly = true)
    public Set<String> getPermissionsForToken(String token) {
        // This method is kept for backward compatibility in PermissionAspect.
        // PermissionAspect should be updated to use getPermissionsByEmail directly.
        // For now, token-based lookup goes through SecurityConfig which sets email.
        return Set.of();
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(String email, String permissionCode) {
        return getPermissionsByEmail(email).contains(permissionCode);
    }

    // ====================== ROLE CRUD ======================

    @Transactional(readOnly = true)
    public CursorPage<RoleResponse> listRoles(UUID cursor, int size) {
        var pageable = PageRequest.of(0, size + 1);
        var items = roleRepository.findWithCursor(cursor, pageable).stream()
                .map(this::toRoleResponse)
                .toList();

        boolean hasNext = items.size() > size;
        if (hasNext) {
            items = items.subList(0, size);
        }
        String nextCursor = hasNext && !items.isEmpty() ? items.getLast().getId().toString() : null;

        return new CursorPage<>(items, nextCursor, hasNext, size, items.size());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID id) {
        var role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return toRoleResponse(role);
    }

    public RoleResponse createRole(RoleCreateRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new BusinessException("DUPLICATE_ROLE", "Role name already exists: " + request.getName());
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isSystem(false)
                .build();

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findByIdIn(List.copyOf(request.getPermissionIds()));
            role.setPermissions(new HashSet<>(permissions));
        }

        var saved = roleRepository.save(role);
        log.info("Role created: {} (id={})", saved.getName(), saved.getId());
        return toRoleResponse(saved);
    }

    public RoleResponse updateRole(UUID id, RoleUpdateRequest request) {
        var role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        if (role.getIsSystem()) {
            throw new BusinessException("SYSTEM_ROLE", "Cannot modify system role");
        }

        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new BusinessException("DUPLICATE_ROLE", "Role name already exists: " + request.getName());
            }
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        if (request.getPermissionIds() != null) {
            List<Permission> permissions = permissionRepository.findByIdIn(List.copyOf(request.getPermissionIds()));
            role.setPermissions(new HashSet<>(permissions));
        }

        role.setUpdatedAt(LocalDateTime.now());
        var saved = roleRepository.save(role);
        log.info("Role updated: {} (id={})", saved.getName(), saved.getId());
        return toRoleResponse(saved);
    }

    public void deleteRole(UUID id) {
        var role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        if (role.getIsSystem()) {
            throw new BusinessException("SYSTEM_ROLE", "Cannot delete system role");
        }

        roleRepository.delete(role);
        log.info("Role deleted: {} (id={})", role.getName(), role.getId());
    }

    // ====================== USER ROLE ASSIGNMENT (by email) ======================

    public void assignRoleToUser(String email, UserRoleAssignRequest request) {
        var role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", request.getRoleId()));

        var adminUser = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "email", email));

        String userId = adminUser.getId().toString();
        var existing = userRoleRepository.findByAdminUserId(userId).orElse(null);
        if (existing != null) {
            existing.setRole(role);
            userRoleRepository.save(existing);
        } else {
            var userRole = AdminUserRole.builder()
                    .adminUserId(userId)
                    .role(role)
                    .build();
            userRoleRepository.save(userRole);
        }
        log.info("Role {} assigned to user {} (email={})", role.getName(), userId, email);
    }

    public void removeRoleFromUser(String email) {
        var adminUser = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "email", email));
        userRoleRepository.deleteByAdminUserId(adminUser.getId().toString());
        log.info("Role removed from user {} (email={})", adminUser.getId(), email);
    }

    // ====================== USER PERMISSION OVERRIDES (by email)
    // ======================

    public void grantPermissionToUser(String email, UserPermissionRequest request) {
        var permission = permissionRepository.findById(request.getPermissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", request.getPermissionId()));

        var adminUser = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "email", email));

        String userId = adminUser.getId().toString();
        var existing = userPermissionRepository.findByAdminUserId(userId).stream()
                .filter(up -> up.getPermission().getId().equals(request.getPermissionId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setIsGranted(true);
            userPermissionRepository.save(existing);
        } else {
            var userPerm = AdminUserPermission.builder()
                    .adminUserId(userId)
                    .permission(permission)
                    .isGranted(true)
                    .build();
            userPermissionRepository.save(userPerm);
        }
        log.info("Permission {} granted to user {} (email={})", permission.getCode(), userId, email);
    }

    public void revokePermissionFromUser(String email, UUID permissionId) {
        var adminUser = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "email", email));
        userPermissionRepository.deleteByAdminUserIdAndPermissionId(adminUser.getId().toString(), permissionId);
        log.info("Permission {} revoked from user {} (email={})", permissionId, adminUser.getId(), email);
    }

    // ====================== USER MANAGEMENT ======================

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(String email) {
        return adminUserRepository.findAll().stream()
                .filter(u -> !u.getEmail().equals(email))
                .map(this::toAdminUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserByEmail(String email) {
        var adminUser = adminUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser", "email", email));
        return toAdminUserResponse(adminUser);
    }

    // ====================== PERMISSIONS LIST ======================

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAllOrdered().stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissionsByModule(String module) {
        return permissionRepository.findByModule(module).stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    // ====================== USER INFO (by email) ======================

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfoByEmail(String email) {
        var adminUser = adminUserRepository.findByEmail(email).orElse(null);
        if (adminUser == null) {
            return UserInfoResponse.builder()
                    .email(email)
                    .permissions(Set.of())
                    .overrides(List.of())
                    .build();
        }

        String userId = adminUser.getId().toString();
        var userRole = userRoleRepository.findByAdminUserId(userId).orElse(null);
        var overrides = userPermissionRepository.findByAdminUserId(userId);
        var effectivePermissions = getPermissionsByEmail(email);

        return UserInfoResponse.builder()
                .email(email)
                .userId(adminUser.getId().toString())
                .displayName(adminUser.getDisplayName())
                .roleId(userRole != null ? userRole.getRole().getId() : null)
                .roleName(userRole != null ? userRole.getRole().getName() : null)
                .permissions(effectivePermissions)
                .overrides(overrides.stream().map(o -> UserPermissionOverrideResponse.builder()
                        .permissionId(o.getPermission().getId())
                        .permissionCode(o.getPermission().getCode())
                        .isGranted(o.getIsGranted())
                        .build()).toList())
                .build();
    }

    // ====================== TO RESPONSE ======================

    private AdminUserResponse toAdminUserResponse(AdminUser user) {
        String userId = user.getId().toString();
        var userRole = userRoleRepository.findByAdminUserId(userId).orElse(null);
        var permissions = getPermissionsByEmail(user.getEmail());

        return AdminUserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .isActive(user.getIsActive())
                .roleName(userRole != null ? userRole.getRole().getName() : null)
                .roleId(userRole != null ? userRole.getRole().getId() : null)
                .permissions(permissions)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private RoleResponse toRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .permissionIds(role.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet()))
                .permissionCodes(role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()))
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    private PermissionResponse toPermissionResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .module(p.getModule())
                .description(p.getDescription())
                .build();
    }
}
