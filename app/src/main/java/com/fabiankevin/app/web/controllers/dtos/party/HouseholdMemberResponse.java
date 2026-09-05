package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.household.HouseholdMemberSummary;
import com.fabiankevin.app.web.controllers.dtos.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a member of a household")
public record HouseholdMemberResponse(
        @Schema(description = "Member identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "User details of the member", exampleClasses =  UserResponse.class)
        UserResponse user,

        @Schema(description = "Whether the member is a household leader", example = "false")
        boolean householdLeader,

        @Schema(description = "Status of the member", example = "ACTIVE")
        String status,

        @Schema(description = "Timestamp when the member joined", example = "2026-01-15T10:30:00Z")
        Instant joinedAt
) {
    public static HouseholdMemberResponse from(HouseholdMemberSummary householdMember) {
        return HouseholdMemberResponse.builder()
                .id(householdMember.id())
                .user(UserResponse.from(householdMember.user()))
                .householdLeader(householdMember.householdLeader())
                .status(householdMember.status() != null ? householdMember.status().name() : null)
                .joinedAt(householdMember.joinedAt())
                .build();
    }
}
