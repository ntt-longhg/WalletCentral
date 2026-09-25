package com.gateway.walletcentral.modules.notification.service;

import com.gateway.walletcentral.core.cursor.CursorPage;
import com.gateway.walletcentral.core.exception.ResourceNotFoundException;
import com.gateway.walletcentral.modules.notification.dto.NotificationResponse;
import com.gateway.walletcentral.modules.notification.model.Notification;
import com.gateway.walletcentral.modules.notification.repository.NotificationRepository;
import com.gateway.walletcentral.modules.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final TenantRepository tenantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
            TenantRepository tenantRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.tenantRepository = tenantRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Save notification and push via WebSocket to the target tenant.
     */
    public Notification saveAndPush(UUID tenantId, String type, String title, String message,
            String referenceType, String referenceId) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        Notification notification = Notification.builder()
                .tenant(tenant)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        var saved = notificationRepository.save(notification);
        log.info("Notification saved: id={} type={} tenant={}", saved.getId(), type, tenantId);

        // Push real-time via WebSocket
        NotificationResponse response = toResponse(saved);
        messagingTemplate.convertAndSend("/topic/notifications/" + tenantId, response);
        // Also push to admin topic for real-time admin dashboard
        messagingTemplate.convertAndSend("/topic/admin/notifications", response);
        log.info("Notification pushed via WebSocket to tenant={} and admin", tenantId);

        return saved;
    }

    @Transactional(readOnly = true)
    public CursorPage<NotificationResponse> list(UUID tenantId, Boolean isRead, UUID cursor, int size) {
        var pageable = PageRequest.of(0, size + 1);
        var items = notificationRepository.findWithCursor(cursor, tenantId, isRead, pageable)
                .stream()
                .map(this::toResponse)
                .toList();

        boolean hasNext = items.size() > size;
        if (hasNext) {
            items = items.subList(0, size);
        }
        String nextCursor = hasNext && !items.isEmpty() ? items.getLast().getId().toString() : null;

        return CursorPage.of(items, nextCursor, hasNext, size);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID tenantId) {
        return notificationRepository.countUnreadByTenantId(tenantId);
    }

    public NotificationResponse markAsRead(UUID id) {
        var notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());

        var saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    public void markAllAsRead(UUID tenantId) {
        notificationRepository.markAllAsReadByTenantId(tenantId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .tenantId(n.getTenant().getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .isRead(n.getIsRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
