package com.fabiankevin.app.web.controllers.dtos.shared_space;

import com.fabiankevin.app.models.shared_space.SpaceParticipantSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a participant in a shared space")
public record ParticipantResponse(
    @Schema(description = "Participant identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    UUID id,

    @Schema(description = "User identifier of the participant", example = "a1b2c3d4-...")
    UUID userId,

    @Schema(description = "Display name of the participant", example = "John Doe")
    String name,

    @Schema(description = "Initials derived from the participant name", example = "JD")
    String initial,

    @Schema(description = "Access level name of the participant", example = "Read & Write")
    String accessLevelName,

    @Schema(description = "Access level description of the participant")
    String accessLevelDescription,

    @Schema(description = "Status of the participant", example = "ACTIVE")
    String status,

    @Schema(description = "Timestamp when the participant joined")
    Instant joinedAt
) {
    public static ParticipantResponse from(SpaceParticipantSummary participant) {
        return ParticipantResponse.builder()
            .id(participant.id())
            .userId(participant.id())
            .name(participant.name())
            .initial(participant.initial())
            .accessLevelName(participant.accessLevel() != null ? participant.accessLevel().getName() : null)
            .accessLevelDescription(participant.accessLevel() != null ? participant.accessLevel().getDescription() : null)
            .status(participant.status() != null ? participant.status().name() : null)
            .joinedAt(participant.joinedAt())
            .build();
    }
}
