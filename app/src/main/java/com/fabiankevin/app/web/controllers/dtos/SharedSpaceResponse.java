package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a shared space")
public record SharedSpaceResponse(
    @Schema(description = "Shared space identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    UUID id,

    @Schema(description = "Display name of the space", example = "Family 2026 Budget")
    String spaceName,

    @Schema(description = "Owner user identifier", example = "a1b2c3d4-...")
    UUID ownerUserId,

    @Schema(description = "Global sharing mode for the space", example = "MUTUAL_SHARING")
    SharingMode sharingMode,

    @Schema(description = "Participants of the space")
    List<ParticipantResponse> participants,

    @Schema(description = "Resources shared into the space")
    List<SharedResourceResponse> sharedResources,

    @Schema(description = "Whether the space is active")
    boolean active,

    @Schema(description = "Timestamp when the space was created")
    Instant createdAt,

    @Schema(description = "Timestamp when the space was last updated")
    Instant updatedAt
) {
    public static SharedSpaceResponse from(SharedSpace space) {
        return SharedSpaceResponse.builder()
            .id(space.id())
            .spaceName(space.spaceName())
            .ownerUserId(space.ownerUserId())
            .sharingMode(space.sharingMode())
            .participants(space.participants().stream().map(ParticipantResponse::from).toList())
            .sharedResources(space.sharedResources().stream().map(SharedResourceResponse::from).toList())
            .active(space.active())
            .createdAt(space.createdAt())
            .updatedAt(space.updatedAt())
            .build();
    }
}
