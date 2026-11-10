package com.fabiankevin.app.clients.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response from the entitlement service")
public record EntitlementClientResponse(
        @Schema(description = "Entitlement identifier", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
        UUID id,

        @Schema(description = "User identifier", example = "d4e5f6a7-b8c9-0123-defa-234567890123")
        UUID userId,

        @Schema(description = "Entitlement feature code", example = "PREMIUM")
        String entitlement,

        @Schema(description = "Whether the entitlement is active", example = "true")
        boolean active,

        @Schema(description = "Entitlement start time", example = "2024-01-01T00:00:00Z")
        Instant startsAt,

        @Schema(description = "Entitlement expiration time", example = "2025-01-01T00:00:00Z")
        Instant expiresAt,

        @Schema(description = "Source subscription identifier", example = "e5f6a7b8-c9d0-1234-efab-345678901234")
        UUID sourceSubscriptionId,

        @Schema(description = "Source type", example = "SUBSCRIPTION")
        String sourceType,

        @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00Z")
        Instant createdAt,

        @Schema(description = "Last update timestamp", example = "2024-01-01T00:00:00Z")
        Instant updatedAt) {
}
