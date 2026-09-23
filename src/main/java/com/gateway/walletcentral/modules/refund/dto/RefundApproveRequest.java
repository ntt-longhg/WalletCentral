package com.gateway.walletcentral.modules.refund.dto;

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
@Schema(description = "Request to approve a refund request")
public class RefundApproveRequest {

    @NotBlank(message = "Reviewed by must not be blank")
    @Schema(description = "Admin who approved the refund", example = "admin")
    private String reviewedBy;
}
