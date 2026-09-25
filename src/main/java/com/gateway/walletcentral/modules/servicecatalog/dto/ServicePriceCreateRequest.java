package com.gateway.walletcentral.modules.servicecatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a service price")
public class ServicePriceCreateRequest {

    @NotNull(message = "Initial size must not be null")
    @Positive(message = "Initial size must be positive")
    @Schema(description = "Initial size in units", example = "100")
    private Integer initialSize;

    @NotNull(message = "Initial fee must not be null")
    @PositiveOrZero(message = "Initial fee must be zero or positive")
    @Schema(description = "Initial fee amount", example = "100.00")
    private BigDecimal initialFee;

    @NotNull(message = "Subsequent size must not be null")
    @Positive(message = "Subsequent size must be positive")
    @Schema(description = "Subsequent size in units", example = "50")
    private Integer subsequentSize;

    @NotNull(message = "Subsequent fee must not be null")
    @PositiveOrZero(message = "Subsequent fee must be zero or positive")
    @Schema(description = "Subsequent fee amount", example = "5.00")
    private BigDecimal subsequentFee;

    @NotNull(message = "Effective date must not be null")
    @Schema(description = "Price effective date")
    private LocalDateTime effectiveDate;
}
