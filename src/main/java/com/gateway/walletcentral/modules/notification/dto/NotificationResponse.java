package com.gateway.walletcentral.modules.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification response")
public class NotificationResponse {

    @Schema(description = "Notification ID")
    private UUID id;

    @Schema(description = "Tenant ID")
    private UUID tenantId;

    @Schema(description = "Notification type", example = "TRANSACTION")
    private String type;

    @Schema(description = "Notification title")
    private String title;

    @Schema(description = "Notification message")
    private String message;

    @Schema(description = "Related entity type")
    private String referenceType;

    @Schema(description = "Related entity ID")
    private String referenceId;

    @Schema(description = "Whether notification has been read")
    private Boolean isRead;

    @Schema(description = "Timestamp when notification was read")
    private LocalDateTime readAt;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}
