package com.gateway.walletcentral.modules.pricingplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pricing plan response")
public class PricingPlanResponse {

    @Schema(description = "Plan ID")
    private UUID id;

    @Schema(description = "Plan code", example = "TOPUP_100")
    private String code;

    @Schema(description = "Plan name", example = "Topup 100")
    private String name;

    @Schema(description = "Plan description")
    private String description;

    @Schema(description = "Plan price", example = "100.00")
    private BigDecimal price;

    @Schema(description = "Plan type", example = "BALANCE_TOPUP")
    private String type;

    @Schema(description = "Bonus type", example = "PERCENTAGE")
    private String bonusType;

    @Schema(description = "Bonus value")
    private BigDecimal bonusValue;

    @Schema(description = "Credit limit action", example = "NONE")
    private String creditLimitAction;

    @Schema(description = "Credit limit value")
    private BigDecimal creditLimitValue;

    @Schema(description = "Plan status", example = "ACTIVE")
    private String status;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
