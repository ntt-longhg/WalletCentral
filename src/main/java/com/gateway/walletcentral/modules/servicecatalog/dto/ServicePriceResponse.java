package com.gateway.walletcentral.modules.servicecatalog.dto;

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
@Schema(description = "Service price response")
public class ServicePriceResponse {

    @Schema(description = "Service price ID")
    private UUID id;

    @Schema(description = "Service ID")
    private UUID serviceId;

    @Schema(description = "Service code")
    private String serviceCode;

    @Schema(description = "Initial size in units")
    private Integer initialSize;

    @Schema(description = "Initial fee amount")
    private BigDecimal initialFee;

    @Schema(description = "Subsequent size in units")
    private Integer subsequentSize;

    @Schema(description = "Subsequent fee amount")
    private BigDecimal subsequentFee;

    @Schema(description = "Whether this price is active")
    private Boolean active;

    @Schema(description = "Price effective date")
    private LocalDateTime effectiveDate;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}
