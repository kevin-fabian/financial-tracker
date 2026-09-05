package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a party")
public record HouseholdResponse(
        @Schema(description = "Party identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "Display name of the party", example = "Family 2026 Budget")
        String name,

        @Schema(description = "Party leader identifier", example = "a1b2c3d4-...")
        UUID leaderId,

        @Schema(description = "Members of the party")
        List<HouseholdMemberResponse> members,

        @Schema(description = "Whether the party is active")
        boolean active,

        @Schema(description = "Timestamp when the party was created")
        Instant createdAt,

        @Schema(description = "Timestamp when the party was last updated")
        Instant updatedAt
) {
    public static HouseholdResponse from(HouseholdSummary party) {
        return HouseholdResponse.builder()
                .id(party.id())
                .name(party.name())
                .leaderId(party.leaderId())
                .members(party.members().stream().map(HouseholdMemberResponse::from).toList())
                .active(party.active())
                .createdAt(party.createdAt())
                .updatedAt(party.updatedAt())
                .build();
    }

    public static HouseholdResponse from(Household household) {
        return HouseholdResponse.builder()
                .id(household.id())
                .name(household.name())
                .leaderId(household.leaderId())
                .members(household.members().stream().map(p ->
                        HouseholdMemberResponse.builder()
                                .id(p.userId())
                                .accessLevelName(p.accessLevel() != null ? p.accessLevel().getName() : null)
                                .accessLevelDescription(p.accessLevel() != null ? p.accessLevel().getDescription() : null)
                                .status(p.status() != null ? p.status().name() : null)
                                .joinedAt(p.joinedAt())
                                .build()).toList())
                .active(household.active())
                .createdAt(household.createdAt())
                .updatedAt(household.updatedAt())
                .build();
    }
}
