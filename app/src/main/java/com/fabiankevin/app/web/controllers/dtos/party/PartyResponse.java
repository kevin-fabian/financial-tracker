package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.models.shared_space.SharedSpaceSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a shared space")
public record PartyResponse(
    @Schema(description = "Shared space identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    UUID id,

    @Schema(description = "Display name of the space", example = "Family 2026 Budget")
    String spaceName,

    @Schema(description = "Owner user identifier", example = "a1b2c3d4-...")
    UUID ownerUserId,

    @Schema(description = "Display name of the sharing mode", example = "Mutual Sharing")
    String sharingModeName,

    @Schema(description = "Description of the sharing mode")
    String sharingModeDescription,

    @Schema(description = "Participants of the space")
    List<PlayerResponse> participants,

    @Schema(description = "Resources shared into the space")
    List<SharedItemResponse> sharedResources,

    @Schema(description = "Whether the space is active")
    boolean active,

    @Schema(description = "Timestamp when the space was created")
    Instant createdAt,

    @Schema(description = "Timestamp when the space was last updated")
    Instant updatedAt
) {
    public static PartyResponse from(SharedSpaceSummary space) {
        return PartyResponse.builder()
            .id(space.id())
            .spaceName(space.spaceName())
            .ownerUserId(space.ownerUserId())
            .sharingModeName(space.sharingMode() != null ? space.sharingMode().getName() : null)
            .sharingModeDescription(space.sharingMode() != null ? space.sharingMode().getDescription() : null)
            .participants(space.participants().stream().map(PlayerResponse::from).toList())
            .sharedResources(space.sharedResources().stream().map(SharedItemResponse::from).toList())
            .active(space.active())
            .createdAt(space.createdAt())
            .updatedAt(space.updatedAt())
            .build();
    }

    public static PartyResponse from(Party space) {
        return PartyResponse.builder()
            .id(space.id())
            .spaceName(space.name())
            .ownerUserId(space.partyLeaderId())
            .sharingModeName(space.sharingMode() != null ? space.sharingMode().getName() : null)
            .sharingModeDescription(space.sharingMode() != null ? space.sharingMode().getDescription() : null)
            .participants(space.participants().stream().map(p ->
                PlayerResponse.builder()
                    .id(p.playerId())
                    .accessLevelName(p.accessLevel() != null ? p.accessLevel().getName() : null)
                    .accessLevelDescription(p.accessLevel() != null ? p.accessLevel().getDescription() : null)
                    .status(p.status() != null ? p.status().name() : null)
                    .joinedAt(p.joinedAt())
                    .build()).toList())
            .sharedResources(space.sharedResources().stream().map(SharedItemResponse::from).toList())
            .active(space.active())
            .createdAt(space.createdAt())
            .updatedAt(space.updatedAt())
            .build();
    }
}
