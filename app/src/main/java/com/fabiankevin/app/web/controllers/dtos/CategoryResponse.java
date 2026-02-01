package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Category;
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
        @Schema(description = "Timestamp when the category was created")
        Instant createdAt,
        @Schema(description = "Timestamp when the category was last updated")
        Instant updatedAt
) {
    public static CategoryResponse from(final Category category) {
        return CategoryResponse.builder()
                .id(category.id())
                .name(category.name())
                .createdAt(category.createdAt())
                .updatedAt(category.updatedAt())
                .build();
    }
}
