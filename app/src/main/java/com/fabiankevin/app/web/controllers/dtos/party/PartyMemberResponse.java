package com.fabiankevin.app.web.controllers.dtos.party;

import com.fabiankevin.app.models.party.PartyMemberSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response representing a participant in a party")
public record PartyMemberResponse(
        @Schema(description = "Participant identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "Player identifier", example = "a1b2c3d4-...")
        UUID playerId,

        @Schema(description = "Display name of the participant", example = "John Doe")
        String name,

        boolean partyLeader,
        boolean partyMember,

        @Schema(description = "Initials derived from the participant name", example = "JD")
        String initial,

        @Schema(description = "Access level name of the participant", example = "Read & Write")
        String accessLevelName,

        @Schema(description = "Access level description of the participant")
        String accessLevelDescription,

        @Schema(description = "Status of the participant", example = "ACTIVE")
        String status,

        @Schema(description = "Average number of transactions per day over the past week")
        double pastWeekDailyAverageTransactionCount,

        @Schema(description = "Number of active budgets the participant owns")
        int activeBudgetCount,

        @Schema(description = "Number of active shopping lists the participant owns")
        int activeShoppingListCount,

        @Schema(description = "Timestamp when the participant joined")
        Instant joinedAt
) {
    public static PartyMemberResponse from(PartyMemberSummary partyMember) {
        return PartyMemberResponse.builder()
                .id(partyMember.id())
                .playerId(partyMember.playerId())
                .name(partyMember.name())
                .partyMember(partyMember.partyMember())
                .partyLeader(partyMember.partyLeader())
                .initial(partyMember.initial())
                .accessLevelName(partyMember.accessLevel() != null ? partyMember.accessLevel().getName() : null)
                .accessLevelDescription(partyMember.accessLevel() != null ? partyMember.accessLevel().getDescription() : null)
                .status(partyMember.status() != null ? partyMember.status().name() : null)
                .pastWeekDailyAverageTransactionCount(partyMember.pastWeekDailyAverageTransactionCount())
                .activeBudgetCount(partyMember.activeBudgetCount())
                .activeShoppingListCount(partyMember.activeShoppingListCount())
                .joinedAt(partyMember.joinedAt())
                .build();
    }
}
