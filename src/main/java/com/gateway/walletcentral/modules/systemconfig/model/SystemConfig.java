package com.gateway.walletcentral.modules.systemconfig.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(
    name = "system_configs",
    comment = "Stores key-value configuration for the system (SMTP, OTP, AUTH settings)",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_system_configs_key", columnNames = "config_key")
    },
    indexes = {
        @Index(name = "idx_system_configs_group", columnList = "config_group")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", updatable = false, nullable = false, comment = "Primary key - UUID v7")
    private UUID id;

    @Column(name = "config_key", nullable = false, unique = true, length = 100, comment = "Configuration key")
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT", comment = "Configuration value")
    private String configValue;

    @Column(name = "config_group", nullable = false, length = 50, comment = "Configuration group (SMTP, OTP, AUTH)")
    private String configGroup;

    @Column(name = "description", length = 255, comment = "Configuration description")
    private String description;

    @Column(name = "field_type", nullable = false, length = 20, comment = "UI field type: text, number, password, textarea, select, radio, boolean, time")
    @Builder.Default
    private String fieldType = "text";

    @Column(name = "field_options", columnDefinition = "TEXT", comment = "JSON options for select/radio: [{\"value\":\"\",\"label\":\"\"}]")
    private String fieldOptions;

    @Column(name = "created_at", updatable = false, comment = "Record creation timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", comment = "Record last update timestamp")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
