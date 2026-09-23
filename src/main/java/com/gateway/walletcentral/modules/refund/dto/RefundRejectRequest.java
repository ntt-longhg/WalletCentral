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
@Schema(description = "Request to reject a refund request")
public class RefundRejectRequest {

    @NotBlank(message = "Reviewed by must not be blank")
    @Schema(description = "Admin who rejected the refund", example = "admin")
    private String reviewedBy;

    @NotBlank(message = "Reject reason must not be blank")
    @Schema(description = "Reason for rejection")
    private String rejectReason;
}
