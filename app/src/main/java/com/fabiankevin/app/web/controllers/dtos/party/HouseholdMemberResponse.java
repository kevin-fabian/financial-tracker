package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.household.HouseholdMemberSummary;
import com.fabiankevin.app.web.controllers.dtos.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a participant in a party")
public record HouseholdMemberResponse(
        @Schema(description = "Participant identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "User details of the participant", exampleClasses =  UserResponse.class)
        UserResponse user,

        @Schema(description = "Whether the member is a party leader", example = "false")
        boolean partyLeader,

        @Schema(description = "Status of the participant", example = "ACTIVE")
        String status,

        @Schema(description = "Timestamp when the participant joined")
        Instant joinedAt
) {
    public static HouseholdMemberResponse from(HouseholdMemberSummary partyMember) {
        return HouseholdMemberResponse.builder()
                .id(partyMember.id())
                .user(UserResponse.from(partyMember.user()))
                .partyLeader(partyMember.partyLeader())
                .status(partyMember.status() != null ? partyMember.status().name() : null)
                .joinedAt(partyMember.joinedAt())
                .build();
    }
}
