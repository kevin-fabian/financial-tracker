package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a household")
public record HouseholdResponse(
        @Schema(description = "Household identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "Display name of the household", example = "Family 2026 Budget")
        String name,

        @Schema(description = "Household leader identifier", example = "a1b2c3d4-...")
        UUID leaderId,

        @Schema(description = "Members of the household")
        List<HouseholdMemberResponse> members,

        @Schema(description = "Whether the household is active", example = "true")
        boolean active,

        @Schema(description = "Timestamp when the household was created", example = "2026-01-15T10:30:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp when the household was last updated", example = "2026-06-20T14:00:00Z")
        Instant updatedAt
) {
    public static HouseholdResponse from(HouseholdSummary household) {
        return HouseholdResponse.builder()
                .id(household.id())
                .name(household.name())
                .leaderId(household.leaderId())
                .members(household.members().stream().map(HouseholdMemberResponse::from).toList())
                .active(household.active())
                .createdAt(household.createdAt())
                .updatedAt(household.updatedAt())
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
                                .status(p.status() != null ? p.status().name() : null)
                                .joinedAt(p.joinedAt())
                                .build()).toList())
                .active(household.active())
                .createdAt(household.createdAt())
                .updatedAt(household.updatedAt())
                .build();
    }
}
