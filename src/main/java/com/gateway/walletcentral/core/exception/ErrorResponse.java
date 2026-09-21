package com.gateway.walletcentral.core.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response body")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error code", example = "VALIDATION_ERROR")
    private String code;

    @Schema(description = "Error message", example = "Validation failed")
    private String message;

    @Schema(description = "Field-level validation errors", example = "{\"name\": \"must not be blank\"}")
    private Map<String, String> errors;

    @Schema(description = "Request tracking ID", example = "abc-123-def-456")
    private String requestId;

    @Schema(description = "Error timestamp")
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    public static ErrorResponse of(int status, String code, String message) {
        return ErrorResponse.builder()
                .status(status)
                .code(code)
                .message(message)
                .requestId(MDC.get("requestId"))
                .build();
    }

    public static ErrorResponse of(int status, String code, String message, Map<String, String> errors) {
        return ErrorResponse.builder()
                .status(status)
                .code(code)
                .message(message)
                .errors(errors)
                .requestId(MDC.get("requestId"))
                .build();
    }
}
