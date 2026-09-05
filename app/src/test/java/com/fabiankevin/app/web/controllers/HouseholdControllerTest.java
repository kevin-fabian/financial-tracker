package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.exceptions.party.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.party.PartyNotFoundException;
import com.fabiankevin.app.models.enums.household.HouseholdMemberStatus;
import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdMemberSummary;
import com.fabiankevin.app.models.household.HouseholdSummary;
import com.fabiankevin.app.services.HouseholdService;
import com.fabiankevin.app.services.InvitationService;
import com.fabiankevin.app.services.commands.party.OrganizeHouseholdCommand;
import com.fabiankevin.app.services.commands.party.PatchPartyCommand;
import com.fabiankevin.app.web.controllers.dtos.party.OrganizeHouseholdRequest;
import com.fabiankevin.app.web.controllers.dtos.party.PatchHouseholdRequest;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.household.AccessLevel.VIEW_ONLY;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class})
@WebMvcTest(PartyController.class)
class HouseholdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HouseholdService householdService;

    @MockitoBean
    private InvitationService invitationService;

    @Autowired
    private JsonMapper jsonMapper;

    private Jwt jwt;
    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        jwt = Jwt.withTokenValue(UUID.randomUUID().toString())
                .subject(userId.toString())
                .header("alg", "RS256")
                .audience(List.of("financial-tracker-test"))
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private HouseholdSummary partyWithId(UUID id, UUID ownerId) {
        return HouseholdSummary.builder()
                .id(id)
                .name("Family 2026 Budget")
                .leaderId(ownerId)
                .members(List.of(leaderSummary(ownerId)))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private HouseholdMemberSummary partyMemberSummary(UUID playerId) {
        return HouseholdMemberSummary.builder()
                .id(UUID.randomUUID())
                .playerId(playerId)
                .name("John Doe")
                .initial("JD")
                .partyLeader(false)
                .partyMember(true)
                .accessLevel(VIEW_ONLY)
                .status(HouseholdMemberStatus.ACTIVE)
                .pastWeekDailyAverageTransactionCount(2.5)
                .activeBudgetCount(1)
                .activeShoppingListCount(3)
                .joinedAt(Instant.now())
                .build();
    }

    private HouseholdMemberSummary leaderSummary(UUID id) {
        return HouseholdMemberSummary.builder()
                .id(UUID.randomUUID())
                .playerId(id)
                .name("Jane Leader")
                .initial("JL")
                .partyLeader(true)
                .partyMember(false)
                .accessLevel(VIEW_ONLY)
                .status(HouseholdMemberStatus.ACTIVE)
                .pastWeekDailyAverageTransactionCount(4.0)
                .activeBudgetCount(2)
                .activeShoppingListCount(1)
                .joinedAt(Instant.now())
                .build();
    }

    @Nested
    class OrganizeHousehold {

        @Test
        void givenValidRequest_thenReturnsCreated() throws Exception {
            UUID partyId = UUID.randomUUID();
            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name("Family 2026 Budget")
                    .build();

            when(householdService.organize(any())).thenReturn(partyWithId(partyId, userId));

            mockMvc.perform(post("/api/parties")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", matchesPattern("http://localhost/api/parties/[-a-f0-9]{36}")))
                    .andExpect(jsonPath("$.id").value(partyId.toString()))
                    .andExpect(jsonPath("$.name").value("Family 2026 Budget"))
                    .andExpect(jsonPath("$.members.length()").value(1))
                    .andExpect(jsonPath("$.members[0].id").exists())
                    .andExpect(jsonPath("$.members[0].userId").value(userId.toString()))
                    .andExpect(jsonPath("$.members[0].partyLeader").value(true))
                    .andExpect(jsonPath("$.members[0].partyMember").value(false));

            verify(householdService).organize(any(OrganizeHouseholdCommand.class));
        }

        @Test
        void givenValidRequest_thenLeaderIsAlsoPartyMember() throws Exception {
            UUID partyId = UUID.randomUUID();
            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name("Family 2026 Budget")
                    .build();

            HouseholdSummary party = HouseholdSummary.builder()
                    .id(partyId)
                    .name("Family 2026 Budget")
                    .leaderId(userId)
                    .members(List.of(leaderSummary(userId)))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdService.organize(any())).thenReturn(party);

            mockMvc.perform(post("/api/parties")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.members[0].partyLeader").value(true))
                    .andExpect(jsonPath("$.members[0].partyMember").value(false));

            verify(householdService).organize(any(OrganizeHouseholdCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name("Family 2026 Budget")

                    .build();

            mockMvc.perform(post("/api/parties")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(householdService);
        }

        @Test
        void givenMissingSharingMode_thenReturnsBadRequest() throws Exception {
            OrganizeHouseholdRequest request = OrganizeHouseholdRequest.builder()
                    .name("Family 2026 Budget")
                    .build();

            mockMvc.perform(post("/api/parties")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(householdService);
        }
    }

    @Nested
    class GetParties {

        @Test
        void givenUserWithSpaces_thenReturnsList() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID partyMemberId = UUID.randomUUID();
            UUID resourceId = UUID.randomUUID();
            HouseholdSummary party = HouseholdSummary.builder()
                    .id(partyId)
                    .name("Family 2026 Budget")
                    .leaderId(userId)
                    .members(List.of(leaderSummary(userId), partyMemberSummary(partyMemberId)))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdService.retrieveByUserId(userId)).thenReturn(List.of(party));

            mockMvc.perform(get("/api/parties")
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(partyId.toString()))
                    .andExpect(jsonPath("$[0].name").value("Family 2026 Budget"))
                    .andExpect(jsonPath("$[0].leaderId").value(userId.toString()))
                    .andExpect(jsonPath("$[0].active").value(true))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists())
                    .andExpect(jsonPath("$[0].members.length()").value(2))
                    .andExpect(jsonPath("$[0].members[0].id").exists())
                    .andExpect(jsonPath("$[0].members[0].userId").value(userId.toString()))
                    .andExpect(jsonPath("$[0].members[0].name").value("Jane Leader"))
                    .andExpect(jsonPath("$[0].members[0].initial").value("JL"))
                    .andExpect(jsonPath("$[0].members[0].partyLeader").value(true))
                    .andExpect(jsonPath("$[0].members[0].partyMember").value(false))
                    .andExpect(jsonPath("$[0].members[0].accessLevelName").value("View Only"))
                    .andExpect(jsonPath("$[0].members[0].accessLevelDescription").value(VIEW_ONLY.getDescription()))
                    .andExpect(jsonPath("$[0].members[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].members[0].pastWeekDailyAverageTransactionCount").value(4.0))
                    .andExpect(jsonPath("$[0].members[0].activeBudgetCount").value(2))
                    .andExpect(jsonPath("$[0].members[0].activeShoppingListCount").value(1))
                    .andExpect(jsonPath("$[0].members[0].joinedAt").exists())
                    .andExpect(jsonPath("$[0].members[1].id").exists())
                    .andExpect(jsonPath("$[0].members[1].userId").value(partyMemberId.toString()))
                    .andExpect(jsonPath("$[0].members[1].name").value("John Doe"))
                    .andExpect(jsonPath("$[0].members[1].initial").value("JD"))
                    .andExpect(jsonPath("$[0].members[1].partyLeader").value(false))
                    .andExpect(jsonPath("$[0].members[1].partyMember").value(true))
                    .andExpect(jsonPath("$[0].members[1].accessLevelName").value("View Only"))
                    .andExpect(jsonPath("$[0].members[1].accessLevelDescription").value(VIEW_ONLY.getDescription()))
                    .andExpect(jsonPath("$[0].members[1].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].members[1].pastWeekDailyAverageTransactionCount").value(2.5))
                    .andExpect(jsonPath("$[0].members[1].activeBudgetCount").value(1))
                    .andExpect(jsonPath("$[0].members[1].activeShoppingListCount").value(3))
                    .andExpect(jsonPath("$[0].members[1].joinedAt").exists())
                    .andExpect(jsonPath("$[0].sharedLoots.length()").value(1));

            verify(householdService).retrieveByUserId(userId);
        }

        @Test
        void givenUserWithSpaces_thenLeaderIsAlsoPartyMember() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID partyMemberId = UUID.randomUUID();
            HouseholdSummary party = HouseholdSummary.builder()
                    .id(partyId)
                    .name("Family 2026 Budget")
                    .leaderId(userId)
                    .members(List.of(leaderSummary(userId), partyMemberSummary(partyMemberId)))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(householdService.retrieveByUserId(userId)).thenReturn(List.of(party));

            mockMvc.perform(get("/api/parties")
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].members[0].partyLeader").value(true))
                    .andExpect(jsonPath("$[0].members[0].partyMember").value(false))
                    .andExpect(jsonPath("$[0].members[1].partyLeader").value(false))
                    .andExpect(jsonPath("$[0].members[1].partyMember").value(true));

            verify(householdService).retrieveByUserId(userId);
        }

        @Test
        void givenNoJwt_thenReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/parties"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(householdService);
        }

        @Test
        void givenUserWithNoSpaces_thenReturnsEmptyList() throws Exception {
            when(householdService.retrieveByUserId(userId)).thenReturn(List.of());

            mockMvc.perform(get("/api/parties")
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class KickHouseholdMember {

        @Test
        void givenOwner_thenReturnsNoContent() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID partyMemberId = UUID.randomUUID();

            mockMvc.perform(delete("/api/parties/" + partyId + "/party-members/" + partyMemberId)
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isNoContent());

            verify(householdService).removeMember(partyId, partyMemberId, userId);
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID partyMemberId = UUID.randomUUID();

            mockMvc.perform(delete("/api/parties/" + partyId + "/party-members/" + partyMemberId))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(householdService);
        }

        @Test
        void givenCannotRemoveOwner_thenReturnsConflict() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID ownerParticipantId = UUID.randomUUID();

            doThrow(new CannotRemoveOwnerException())
                    .when(householdService).removeMember(partyId, ownerParticipantId, userId);

            mockMvc.perform(delete("/api/parties/" + partyId + "/party-members/" + ownerParticipantId)
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class DisbandHousehold {

        @Test
        void givenOwner_thenReturnsNoContent() throws Exception {
            UUID partyId = UUID.randomUUID();

            mockMvc.perform(delete("/api/parties/" + partyId)
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isNoContent());

            verify(householdService).disbandHousehold(partyId, userId);
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();

            mockMvc.perform(delete("/api/parties/" + partyId))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(householdService);
        }

        @Test
        void givenSpaceNotFound_thenReturnsNotFound() throws Exception {
            UUID partyId = UUID.randomUUID();

            doThrow(new PartyNotFoundException())
                    .when(householdService).disbandHousehold(partyId, userId);

            mockMvc.perform(delete("/api/parties/" + partyId)
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class PatchHousehold {

        @Test
        void givenValidRequest_thenReturnsUpdated() throws Exception {
            UUID partyId = UUID.randomUUID();
            PatchHouseholdRequest request = PatchHouseholdRequest.builder()
                    .partyName("Updated Budget")
                    .build();

            when(householdService.patchHousehold(any())).thenAnswer(invocation -> {
                var command = invocation.getArgument(0, PatchPartyCommand.class);
                return Household.builder()
                        .id(command.id())
                        .name(command.partyName())
                        .leaderId(userId)
                        .members(List.of())
                        .active(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
            });

            mockMvc.perform(patch("/api/parties/" + partyId)
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(partyId.toString()))
                    .andExpect(jsonPath("$.name").value("Updated Budget"));

            verify(householdService).patchHousehold(any());
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();
            PatchHouseholdRequest request = PatchHouseholdRequest.builder()
                    .partyName("Updated Budget")
                    .build();

            mockMvc.perform(patch("/api/parties/" + partyId)
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(householdService);
        }

        @Test
        void givenSpaceNotFound_thenReturnsNotFound() throws Exception {
            UUID partyId = UUID.randomUUID();
            PatchHouseholdRequest request = PatchHouseholdRequest.builder()
                    .partyName("Updated Budget")
                    .build();

            doThrow(new PartyNotFoundException())
                    .when(householdService).patchHousehold(any());

            mockMvc.perform(patch("/api/parties/" + partyId)
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
