package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.exceptions.shared_space.CannotRemoveOwnerException;
import com.fabiankevin.app.exceptions.shared_space.SharedSpaceNotFoundException;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpaceSummary;
import com.fabiankevin.app.models.shared_space.SpaceParticipantSummary;
import com.fabiankevin.app.services.InvitationService;
import com.fabiankevin.app.services.PartyService;
import com.fabiankevin.app.services.commands.shared_space.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.shared_space.PatchPartyCommand;
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

import static com.fabiankevin.app.models.enums.shared_space.AccessLevel.READ_WRITE;
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

    private Party spaceWithId(UUID id, UUID ownerId) {
        return Party.builder()
            .id(id)
            .name("Family 2026 Budget")
            .partyLeaderId(ownerId)
            .sharingMode(SharingMode.EVEN_SHARE)
            .participants(List.of())
            .sharedResources(List.of())
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private SpaceParticipantSummary participantSummary(UUID id) {
        return SpaceParticipantSummary.builder()
            .id(id)
            .name("John Doe")
            .initial("JD")
            .accessLevel(AccessLevel.READ_WRITE)
            .status(ParticipantStatus.ACTIVE)
            .joinedAt(Instant.now())
            .build();
    }

    private SharedResource sharedResource(UUID id, ResourceType type) {
        return SharedResource.builder()
            .id(id)
            .type(type)
            .items(List.of("item-1", "item-2"))
            .sharedAt(Instant.now())
            .build();
    }

    @Nested
    class CreateParty {

        @Test
        void givenValidRequest_thenReturnsCreated() throws Exception {
            UUID spaceId = UUID.randomUUID();
            OrganizePartyRequest request = OrganizePartyRequest.builder()
                .spaceName("Family 2026 Budget")
                .sharingMode(SharingMode.EVEN_SHARE)
                .build();

            when(partyService.organize(any())).thenReturn(spaceWithId(spaceId, userId));

            mockMvc.perform(post("/api/shared-spaces")
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("http://localhost/api/shared-spaces/[-a-f0-9]{36}")))
                .andExpect(jsonPath("$.id").value(spaceId.toString()))
                .andExpect(jsonPath("$.name").value("Family 2026 Budget"));

            verify(partyService).organize(any(OrganizePartyCommand.class));
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            OrganizePartyRequest request = OrganizePartyRequest.builder()
                .spaceName("Family 2026 Budget")
                .sharingMode(SharingMode.EVEN_SHARE)
                .build();

            mockMvc.perform(post("/api/shared-spaces")
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenMissingSharingMode_thenReturnsBadRequest() throws Exception {
            OrganizePartyRequest request = OrganizePartyRequest.builder()
                .spaceName("Family 2026 Budget")
                .build();

            mockMvc.perform(post("/api/shared-spaces")
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(partyService);
        }
    }

    @Nested
    class GetSharedSpaces {

        @Test
        void givenUserWithSpaces_thenReturnsList() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();
            UUID resourceId = UUID.randomUUID();
            SharedSpaceSummary space = SharedSpaceSummary.builder()
                .id(spaceId)
                .spaceName("Family 2026 Budget")
                .ownerUserId(userId)
                .sharingMode(SharingMode.EVEN_SHARE)
                .participants(List.of(participantSummary(participantId)))
                .sharedResources(List.of(sharedResource(resourceId, ResourceType.TRANSACTION)))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            when(partyService.retrieveByUserId(userId)).thenReturn(List.of(space));

            mockMvc.perform(get("/api/shared-spaces")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(spaceId.toString()))
                .andExpect(jsonPath("$[0].name").value("Family 2026 Budget"))
                .andExpect(jsonPath("$[0].partyLeaderId").value(userId.toString()))
                .andExpect(jsonPath("$[0].sharingModeName").value("Mutual Sharing"))
                .andExpect(jsonPath("$[0].sharingModeDescription").value(SharingMode.EVEN_SHARE.getDescription()))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].participants.length()").value(1))
                .andExpect(jsonPath("$[0].participants[0].id").value(participantId.toString()))
                .andExpect(jsonPath("$[0].participants[0].playerId").value(participantId.toString()))
                .andExpect(jsonPath("$[0].participants[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].participants[0].initial").value("JD"))
                .andExpect(jsonPath("$[0].participants[0].accessLevelName").value("Read & Write"))
                .andExpect(jsonPath("$[0].participants[0].accessLevelDescription").value(READ_WRITE.getDescription()))
                .andExpect(jsonPath("$[0].participants[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].participants[0].joinedAt").exists())
                .andExpect(jsonPath("$[0].sharedResources.length()").value(1))
                .andExpect(jsonPath("$[0].sharedResources[0].id").value(resourceId.toString()))
                .andExpect(jsonPath("$[0].sharedResources[0].type").value("TRANSACTION"))
                .andExpect(jsonPath("$[0].sharedResources[0].name").value("Transaction"))
                .andExpect(jsonPath("$[0].sharedResources[0].description").value(ResourceType.TRANSACTION.getDescription()))
                .andExpect(jsonPath("$[0].sharedResources[0].items.length()").value(2))
                .andExpect(jsonPath("$[0].sharedResources[0].items[0]").value("item-1"))
                .andExpect(jsonPath("$[0].sharedResources[0].sharedAt").exists());

            verify(partyService).retrieveByUserId(userId);
        }

        @Test
        void givenNoJwt_thenReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/shared-spaces"))
                .andExpect(status().isUnauthorized());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenUserWithNoSpaces_thenReturnsEmptyList() throws Exception {
            when(partyService.retrieveByUserId(userId)).thenReturn(List.of());

            mockMvc.perform(get("/api/shared-spaces")
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class RemoveParticipant {

        @Test
        void givenOwner_thenReturnsNoContent() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();

            mockMvc.perform(delete("/api/shared-spaces/" + spaceId + "/participants/" + participantId)
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isNoContent());

            verify(partyService).removeParticipant(spaceId, participantId, userId);
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID participantId = UUID.randomUUID();

            mockMvc.perform(delete("/api/shared-spaces/" + spaceId + "/participants/" + participantId))
                .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenCannotRemoveOwner_thenReturnsConflict() throws Exception {
            UUID spaceId = UUID.randomUUID();
            UUID ownerParticipantId = UUID.randomUUID();

            doThrow(new CannotRemoveOwnerException())
                .when(partyService).removeParticipant(spaceId, ownerParticipantId, userId);

            mockMvc.perform(delete("/api/shared-spaces/" + spaceId + "/participants/" + ownerParticipantId)
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    class DeleteParty {

        @Test
        void givenOwner_thenReturnsNoContent() throws Exception {
            UUID spaceId = UUID.randomUUID();

            mockMvc.perform(delete("/api/shared-spaces/" + spaceId)
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isNoContent());

            verify(partyService).deleteSharedSpace(spaceId, userId);
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID spaceId = UUID.randomUUID();

            mockMvc.perform(delete("/api/shared-spaces/" + spaceId))
                .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenSpaceNotFound_thenReturnsNotFound() throws Exception {
            UUID spaceId = UUID.randomUUID();

            doThrow(new SharedSpaceNotFoundException())
                .when(partyService).deleteSharedSpace(spaceId, userId);

            mockMvc.perform(delete("/api/shared-spaces/" + spaceId)
                    .with(jwt().jwt(jwt)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    class PatchParty {

        @Test
        void givenValidRequest_thenReturnsUpdated() throws Exception {
            UUID spaceId = UUID.randomUUID();
            PatchPartyRequest request = PatchPartyRequest.builder()
                .partyName("Updated Budget")
                .sharingMode(SharingMode.EVEN_SHARE)
                .build();

            when(partyService.patchSharedSpace(any())).thenAnswer(invocation -> {
                var command = invocation.getArgument(0, PatchPartyCommand.class);
                return Party.builder()
                    .id(command.id())
                    .name(command.partyName())
                    .partyLeaderId(userId)
                    .sharingMode(command.sharingMode())
                    .participants(List.of())
                    .sharedResources(List.of())
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            });

            mockMvc.perform(patch("/api/shared-spaces/" + spaceId)
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(spaceId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Budget"));

            verify(partyService).patchSharedSpace(any());
        }

        @Test
        void givenMissingJwt_thenReturnsForbidden() throws Exception {
            UUID spaceId = UUID.randomUUID();
            PatchPartyRequest request = PatchPartyRequest.builder()
                .partyName("Updated Budget")
                .build();

            mockMvc.perform(patch("/api/shared-spaces/" + spaceId)
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

            verifyNoInteractions(partyService);
        }

        @Test
        void givenSpaceNotFound_thenReturnsNotFound() throws Exception {
            UUID spaceId = UUID.randomUUID();
            PatchPartyRequest request = PatchPartyRequest.builder()
                .partyName("Updated Budget")
                .build();

            doThrow(new SharedSpaceNotFoundException())
                .when(partyService).patchSharedSpace(any());

            mockMvc.perform(patch("/api/shared-spaces/" + spaceId)
                    .with(jwt().jwt(jwt))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }
}
