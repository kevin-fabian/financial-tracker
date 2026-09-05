package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO representing a category record")
public record CategoryResponse(
        @Schema(description = "Unique identifier of the category", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "Name of the category", example = "FOOD")
        String name,
        @Schema(description = "Transaction type of the category", example = "EXPENSE")
        TransactionType type,
        @Schema(description = "Icon for the category", example = "restaurant")
        String icon,
        @Schema(description = "Whether the category is active", example = "true")
        boolean active,
        @Schema(description = "Whether the category is a system category", example = "false")
        boolean system,
        @Schema(description = "Timestamp when the category was created", example = "2025-01-01T00:00:00Z")
        Instant createdAt,
        @Schema(description = "Timestamp when the category was last updated", example = "2025-06-15T10:30:00Z")
        Instant updatedAt
) {
    public static CategoryResponse from(final Category category) {
        if (category == null) {
            return null;
        }
        String icon = category.icon() != null ? category.icon() : null;
        return CategoryResponse.builder()
                .id(category.id())
                .name(category.name())
                .type(category.type())
                .icon(icon)
                .active(category.active())
                .system(category.system())
                .createdAt(category.createdAt())
                .updatedAt(category.updatedAt())
                .build();
    }
}
