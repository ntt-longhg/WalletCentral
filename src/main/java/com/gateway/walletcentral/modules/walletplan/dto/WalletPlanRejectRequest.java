package com.gateway.walletcentral.modules.walletplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to reject a wallet plan")
public class WalletPlanRejectRequest {

    @NotBlank(message = "Approved by must not be blank")
    @Schema(description = "Reviewer identifier", example = "admin")
    private String approvedBy;

    @NotBlank(message = "Reject reason must not be blank")
    @Schema(description = "Reason for rejection")
    private String rejectReason;
}
