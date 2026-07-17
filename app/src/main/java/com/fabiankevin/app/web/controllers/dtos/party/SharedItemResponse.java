package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.shared_space.SharedItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a resource shared into a space")
public record SharedItemResponse(
        @Schema(description = "Shared resource identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "Type of the shared resource", example = "TRANSACTION")
        String type,

        @Schema(description = "Display name of the resource type", example = "Transaction")
        String name,

        @Schema(description = "Description of the resource type")
        String description,

        @Schema(description = "Identifiers of the shared items")
        List<String> items,

        @Schema(description = "Timestamp when the resource was shared")
        Instant sharedAt
) {
    public static SharedItemResponse from(SharedItem resource) {
        return SharedItemResponse.builder()
                .id(resource.id())
                .type(resource.type() != null ? resource.type().name() : null)
                .name(resource.type() != null ? resource.type().getName() : null)
                .description(resource.type() != null ? resource.type().getDescription() : null)
                .items(resource.items())
                .sharedAt(resource.sharedAt())
                .build();
    }
}
