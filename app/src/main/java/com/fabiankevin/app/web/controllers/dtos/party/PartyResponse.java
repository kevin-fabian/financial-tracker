package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.models.shared_space.PartySummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a party")
public record PartyResponse(
    @Schema(description = "Party identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    UUID id,

    @Schema(description = "Display name of the party", example = "Family 2026 Budget")
    String name,

    @Schema(description = "Party leader identifier", example = "a1b2c3d4-...")
    UUID partyLeaderId,

    @Schema(description = "Display name of the sharing mode", example = "Even Share")
    String sharingModeName,

    @Schema(description = "Description of the sharing mode")
    String sharingModeDescription,

    @Schema(description = "Members of the party")
    List<PartyMemberResponse> partyMembers,

    @Schema(description = "Resources shared into the party")
    List<SharedItemResponse> sharedResources,

    @Schema(description = "Whether the party is active")
    boolean active,

    @Schema(description = "Timestamp when the party was created")
    Instant createdAt,

    @Schema(description = "Timestamp when the party was last updated")
    Instant updatedAt
) {
    public static PartyResponse from(PartySummary party) {
        return PartyResponse.builder()
            .id(party.id())
            .name(party.name())
            .partyLeaderId(party.partyLeaderId())
            .sharingModeName(party.sharingMode() != null ? party.sharingMode().getName() : null)
            .sharingModeDescription(party.sharingMode() != null ? party.sharingMode().getDescription() : null)
            .partyMembers(party.participants().stream().map(PartyMemberResponse::from).toList())
            .sharedResources(party.sharedItems().stream().map(SharedItemResponse::from).toList())
            .active(party.active())
            .createdAt(party.createdAt())
            .updatedAt(party.updatedAt())
            .build();
    }

    public static PartyResponse from(Party party) {
        return PartyResponse.builder()
            .id(party.id())
            .name(party.name())
            .partyLeaderId(party.partyLeaderId())
            .sharingModeName(party.sharingMode() != null ? party.sharingMode().getName() : null)
            .sharingModeDescription(party.sharingMode() != null ? party.sharingMode().getDescription() : null)
            .partyMembers(party.partyMembers().stream().map(p ->
                PartyMemberResponse.builder()
                    .id(p.playerId())
                    .accessLevelName(p.accessLevel() != null ? p.accessLevel().getName() : null)
                    .accessLevelDescription(p.accessLevel() != null ? p.accessLevel().getDescription() : null)
                    .status(p.status() != null ? p.status().name() : null)
                    .joinedAt(p.joinedAt())
                    .build()).toList())
            .sharedResources(party.sharedItems().stream().map(SharedItemResponse::from).toList())
            .active(party.active())
            .createdAt(party.createdAt())
            .updatedAt(party.updatedAt())
            .build();
    }
}
