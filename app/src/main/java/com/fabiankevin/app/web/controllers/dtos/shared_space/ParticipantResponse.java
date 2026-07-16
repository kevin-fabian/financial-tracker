package com.fabiankevin.app.web.controllers.dtos.shared_space;

import com.fabiankevin.app.models.shared_space.SpaceParticipant;
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

    String name,

    String initial,

    @Schema(description = "Access level of the participant", example = "READ_WRITE")
    String accessLevel,

    @Schema(description = "Status of the participant", example = "ACTIVE")
    String status,

    @Schema(description = "Timestamp when the participant joined")
    Instant joinedAt,

    int transactionCount,
    double averageAmount,
    int goalCount,
    int checklistCount
) {
    public static ParticipantResponse from(SpaceParticipant participant) {
        return ParticipantResponse.builder()
            .id(participant.id())
            .userId(participant.userId())
            .accessLevel(participant.accessLevel() != null ? participant.accessLevel().name() : null)
            .status(participant.status() != null ? participant.status().name() : null)
            .joinedAt(participant.joinedAt())
            .build();
    }
}
