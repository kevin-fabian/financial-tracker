package com.fabiankevin.app.clients.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to revoke an entitlement")
public record RevokeEntitlementRequest(
        @Schema(description = "Entitlement name to revoke", example = "PARTNERSHIP")
        @NotBlank(message = "entitlement is required")
        @Size(max = 31, message = "entitlement must not exceed 31 characters")
        String entitlement) {
}
