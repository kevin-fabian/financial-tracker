package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.exceptions.party.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.party.PartyNotFoundException;
import com.fabiankevin.app.models.enums.party.PartyMemberStatus;
import com.fabiankevin.app.models.enums.party.ResourceType;
import com.fabiankevin.app.models.enums.party.SharingMode;
import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.models.party.PartyMemberSummary;
import com.fabiankevin.app.models.party.PartySummary;
import com.fabiankevin.app.models.party.SharedItem;
import com.fabiankevin.app.services.InvitationService;
import com.fabiankevin.app.services.PartyService;
import com.fabiankevin.app.services.commands.party.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.party.PatchPartyCommand;
import com.fabiankevin.app.web.controllers.dtos.party.OrganizePartyRequest;
import com.fabiankevin.app.web.controllers.dtos.party.PatchPartyRequest;
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

import static com.fabiankevin.app.models.enums.party.AccessLevel.VIEW_ONLY;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class})
@WebMvcTest(PartyController.class)
class PartyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartyService partyService;

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

    private PartySummary partyWithId(UUID id, UUID ownerId) {
        return PartySummary.builder()
                .id(id)
                .name("Family 2026 Budget")
                .partyLeaderId(ownerId)
                .sharingMode(SharingMode.EVEN_SHARE)
                .partyMembers(List.of(leaderSummary(ownerId)))
                .sharedItems(List.of())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private PartyMemberSummary partyMemberSummary(UUID playerId) {
        return PartyMemberSummary.builder()
                .id(UUID.randomUUID())
                .playerId(playerId)
                .name("John Doe")
                .initial("JD")
                .partyLeader(false)
                .partyMember(true)
                .accessLevel(VIEW_ONLY)
                .status(PartyMemberStatus.ACTIVE)
                .pastWeekDailyAverageTransactionCount(2.5)
                .activeBudgetCount(1)
                .activeShoppingListCount(3)
                .joinedAt(Instant.now())
                .build();
    }

    private PartyMemberSummary leaderSummary(UUID id) {
        return PartyMemberSummary.builder()
                .id(UUID.randomUUID())
                .playerId(id)
                .name("Jane Leader")
                .initial("JL")
                .partyLeader(true)
                .partyMember(false)
                .accessLevel(VIEW_ONLY)
                .status(PartyMemberStatus.ACTIVE)
                .pastWeekDailyAverageTransactionCount(4.0)
                .activeBudgetCount(2)
                .activeShoppingListCount(1)
                .joinedAt(Instant.now())
                .build();
    }

    private SharedItem sharedResource(UUID id, ResourceType type) {
        return SharedItem.builder()
                .id(id)
                .type(type)
                .items(List.of("item-1", "item-2"))
                .sharedAt(Instant.now())
                .build();
    }

    @Nested
    class OrganizeParty {

        @Test
        void givenValidRequest_thenReturnsCreated() throws Exception {
            UUID partyId = UUID.randomUUID();
            OrganizePartyRequest request = OrganizePartyRequest.builder()
                    .name("Family 2026 Budget")
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build();

            when(partyService.organize(any())).thenReturn(partyWithId(partyId, userId));

            mockMvc.perform(post("/api/parties")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", matchesPattern("http://localhost/api/parties/[-a-f0-9]{36}")))
                    .andExpect(jsonPath("$.id").value(partyId.toString()))
                    .andExpect(jsonPath("$.name").value("Family 2026 Budget"))
                    .andExpect(jsonPath("$.partyMembers.length()").value(1))
                    .andExpect(jsonPath("$.partyMembers[0].id").exists())
                    .andExpect(jsonPath("$.partyMembers[0].playerId").value(userId.toString()))
                    .andExpect(jsonPath("$.partyMembers[0].partyLeader").value(true))
                    .andExpect(jsonPath("$.partyMembers[0].partyMember").value(false));

            verify(partyService).organize(any(OrganizePartyCommand.class));
        }

        @Test
        void givenValidRequest_thenLeaderIsAlsoPartyMember() throws Exception {
            UUID partyId = UUID.randomUUID();
            OrganizePartyRequest request = OrganizePartyRequest.builder()
                    .name("Family 2026 Budget")
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build();

            PartySummary party = PartySummary.builder()
                    .id(partyId)
                    .name("Family 2026 Budget")
                    .partyLeaderId(userId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .partyMembers(List.of(leaderSummary(userId)))
                    .sharedItems(List.of())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(partyService.organize(any())).thenReturn(party);

            mockMvc.perform(post("/api/parties")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.partyMembers[0].partyLeader").value(true))
                    .andExpect(jsonPath("$.partyMembers[0].partyMember").value(false));

            verify(partyService).organize(any(OrganizePartyCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            OrganizePartyRequest request = OrganizePartyRequest.builder()
                    .name("Family 2026 Budget")
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build();

            mockMvc.perform(post("/api/parties")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenMissingSharingMode_thenReturnsBadRequest() throws Exception {
            OrganizePartyRequest request = OrganizePartyRequest.builder()
                    .name("Family 2026 Budget")
                    .build();

            mockMvc.perform(post("/api/parties")
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(partyService);
        }
    }

    @Nested
    class GetParties {

        @Test
        void givenUserWithSpaces_thenReturnsList() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID partyMemberId = UUID.randomUUID();
            UUID resourceId = UUID.randomUUID();
            PartySummary party = PartySummary.builder()
                    .id(partyId)
                    .name("Family 2026 Budget")
                    .partyLeaderId(userId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .partyMembers(List.of(leaderSummary(userId), partyMemberSummary(partyMemberId)))
                    .sharedItems(List.of(sharedResource(resourceId, ResourceType.TRANSACTION)))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(partyService.retrieveByUserId(userId)).thenReturn(List.of(party));

            mockMvc.perform(get("/api/parties")
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(partyId.toString()))
                    .andExpect(jsonPath("$[0].name").value("Family 2026 Budget"))
                    .andExpect(jsonPath("$[0].partyLeaderId").value(userId.toString()))
                    .andExpect(jsonPath("$[0].sharingModeName").value("Even Share"))
                    .andExpect(jsonPath("$[0].sharingModeDescription").value(SharingMode.EVEN_SHARE.getDescription()))
                    .andExpect(jsonPath("$[0].active").value(true))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[0].updatedAt").exists())
                    .andExpect(jsonPath("$[0].partyMembers.length()").value(2))
                    .andExpect(jsonPath("$[0].partyMembers[0].id").exists())
                    .andExpect(jsonPath("$[0].partyMembers[0].playerId").value(userId.toString()))
                    .andExpect(jsonPath("$[0].partyMembers[0].name").value("Jane Leader"))
                    .andExpect(jsonPath("$[0].partyMembers[0].initial").value("JL"))
                    .andExpect(jsonPath("$[0].partyMembers[0].partyLeader").value(true))
                    .andExpect(jsonPath("$[0].partyMembers[0].partyMember").value(false))
                    .andExpect(jsonPath("$[0].partyMembers[0].accessLevelName").value("Full Co-op"))
                    .andExpect(jsonPath("$[0].partyMembers[0].accessLevelDescription").value(VIEW_ONLY.getDescription()))
                    .andExpect(jsonPath("$[0].partyMembers[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].partyMembers[0].pastWeekDailyAverageTransactionCount").value(4.0))
                    .andExpect(jsonPath("$[0].partyMembers[0].activeBudgetCount").value(2))
                    .andExpect(jsonPath("$[0].partyMembers[0].activeShoppingListCount").value(1))
                    .andExpect(jsonPath("$[0].partyMembers[0].joinedAt").exists())
                    .andExpect(jsonPath("$[0].partyMembers[1].id").exists())
                    .andExpect(jsonPath("$[0].partyMembers[1].playerId").value(partyMemberId.toString()))
                    .andExpect(jsonPath("$[0].partyMembers[1].name").value("John Doe"))
                    .andExpect(jsonPath("$[0].partyMembers[1].initial").value("JD"))
                    .andExpect(jsonPath("$[0].partyMembers[1].partyLeader").value(false))
                    .andExpect(jsonPath("$[0].partyMembers[1].partyMember").value(true))
                    .andExpect(jsonPath("$[0].partyMembers[1].accessLevelName").value("Full Co-op"))
                    .andExpect(jsonPath("$[0].partyMembers[1].accessLevelDescription").value(VIEW_ONLY.getDescription()))
                    .andExpect(jsonPath("$[0].partyMembers[1].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].partyMembers[1].pastWeekDailyAverageTransactionCount").value(2.5))
                    .andExpect(jsonPath("$[0].partyMembers[1].activeBudgetCount").value(1))
                    .andExpect(jsonPath("$[0].partyMembers[1].activeShoppingListCount").value(3))
                    .andExpect(jsonPath("$[0].partyMembers[1].joinedAt").exists())
                    .andExpect(jsonPath("$[0].sharedLoots.length()").value(1))
                    .andExpect(jsonPath("$[0].sharedLoots[0].id").value(resourceId.toString()))
                    .andExpect(jsonPath("$[0].sharedLoots[0].type").value("TRANSACTION"))
                    .andExpect(jsonPath("$[0].sharedLoots[0].name").value("Party Loot(Transactions)"))
                    .andExpect(jsonPath("$[0].sharedLoots[0].description").value(ResourceType.TRANSACTION.getDescription()))
                    .andExpect(jsonPath("$[0].sharedLoots[0].items.length()").value(2))
                    .andExpect(jsonPath("$[0].sharedLoots[0].items[0]").value("item-1"))
                    .andExpect(jsonPath("$[0].sharedLoots[0].sharedAt").exists());

            verify(partyService).retrieveByUserId(userId);
        }

        @Test
        void givenUserWithSpaces_thenLeaderIsAlsoPartyMember() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID partyMemberId = UUID.randomUUID();
            PartySummary party = PartySummary.builder()
                    .id(partyId)
                    .name("Family 2026 Budget")
                    .partyLeaderId(userId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .partyMembers(List.of(leaderSummary(userId), partyMemberSummary(partyMemberId)))
                    .sharedItems(List.of())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(partyService.retrieveByUserId(userId)).thenReturn(List.of(party));

            mockMvc.perform(get("/api/parties")
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].partyMembers[0].partyLeader").value(true))
                    .andExpect(jsonPath("$[0].partyMembers[0].partyMember").value(false))
                    .andExpect(jsonPath("$[0].partyMembers[1].partyLeader").value(false))
                    .andExpect(jsonPath("$[0].partyMembers[1].partyMember").value(true));

            verify(partyService).retrieveByUserId(userId);
        }

        @Test
        void givenNoJwt_thenReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/parties"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenUserWithNoSpaces_thenReturnsEmptyList() throws Exception {
            when(partyService.retrieveByUserId(userId)).thenReturn(List.of());

            mockMvc.perform(get("/api/parties")
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class KickPartyMember {

        @Test
        void givenOwner_thenReturnsNoContent() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID partyMemberId = UUID.randomUUID();

            mockMvc.perform(delete("/api/parties/" + partyId + "/party-members/" + partyMemberId)
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isNoContent());

            verify(partyService).kickPartyMember(partyId, partyMemberId, userId);
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID partyMemberId = UUID.randomUUID();

            mockMvc.perform(delete("/api/parties/" + partyId + "/party-members/" + partyMemberId))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenCannotRemoveOwner_thenReturnsConflict() throws Exception {
            UUID partyId = UUID.randomUUID();
            UUID ownerParticipantId = UUID.randomUUID();

            doThrow(new CannotRemoveOwnerException())
                    .when(partyService).kickPartyMember(partyId, ownerParticipantId, userId);

            mockMvc.perform(delete("/api/parties/" + partyId + "/party-members/" + ownerParticipantId)
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class DisbandParty {

        @Test
        void givenOwner_thenReturnsNoContent() throws Exception {
            UUID partyId = UUID.randomUUID();

            mockMvc.perform(delete("/api/parties/" + partyId)
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isNoContent());

            verify(partyService).disbandParty(partyId, userId);
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();

            mockMvc.perform(delete("/api/parties/" + partyId))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenSpaceNotFound_thenReturnsNotFound() throws Exception {
            UUID partyId = UUID.randomUUID();

            doThrow(new PartyNotFoundException())
                    .when(partyService).disbandParty(partyId, userId);

            mockMvc.perform(delete("/api/parties/" + partyId)
                            .with(jwt().jwt(jwt)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class PatchParty {

        @Test
        void givenValidRequest_thenReturnsUpdated() throws Exception {
            UUID partyId = UUID.randomUUID();
            PatchPartyRequest request = PatchPartyRequest.builder()
                    .partyName("Updated Budget")
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build();

            when(partyService.patchParty(any())).thenAnswer(invocation -> {
                var command = invocation.getArgument(0, PatchPartyCommand.class);
                return Party.builder()
                        .id(command.id())
                        .name(command.partyName())
                        .partyLeaderId(userId)
                        .sharingMode(command.sharingMode())
                        .partyMembers(List.of())
                        .sharedItems(List.of())
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

            verify(partyService).patchParty(any());
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID partyId = UUID.randomUUID();
            PatchPartyRequest request = PatchPartyRequest.builder()
                    .partyName("Updated Budget")
                    .build();

            mockMvc.perform(patch("/api/parties/" + partyId)
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenSpaceNotFound_thenReturnsNotFound() throws Exception {
            UUID partyId = UUID.randomUUID();
            PatchPartyRequest request = PatchPartyRequest.builder()
                    .partyName("Updated Budget")
                    .build();

            doThrow(new PartyNotFoundException())
                    .when(partyService).patchParty(any());

            mockMvc.perform(patch("/api/parties/" + partyId)
                            .with(jwt().jwt(jwt))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
