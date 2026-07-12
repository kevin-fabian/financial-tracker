package com.fabiankevin.app.services;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SharingRule;
import com.fabiankevin.app.services.commands.shared_space.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.AddSharedResourceCommand;
import com.fabiankevin.app.services.commands.shared_space.CreateSharedSpaceCommand;
import com.fabiankevin.app.services.commands.shared_space.SendInvitationCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

@SpringBootTest
public class DefaultSharedSpaceServiceSpringBootTest {
    @Autowired
    private SharedSpaceService sharedSpaceService;

    @DisplayName("")
    @Test
    void mutualSharingFlow() {
        UUID ownerUserId = UUID.randomUUID();
        UUID partnerUserid = UUID.randomUUID();

        // Step 1: Create a space
        SharedSpace initialSharedSpace = sharedSpaceService.createShare(CreateSharedSpaceCommand.builder()
                .spaceName("Partner Space")
                .ownerUserId(ownerUserId)
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .resources(List.of(AddSharedResourceCommand.builder()
                                .type(ResourceType.TRANSACTION)
                                .itemIds(List.of("INCOME", "EXPENSES"))
                                .build(),
                        AddSharedResourceCommand.builder()
                                .type(ResourceType.BUDGET)
                                .itemIds(List.of(UUID.randomUUID().toString()))
                                .build()))
                .build());
        UUID spaceId = initialSharedSpace.id();

        // Step 2: Invite a partner in a space
        Invitation invitation = sharedSpaceService.sendInvitation(SendInvitationCommand.builder()
                .spaceId(spaceId)
                .inviterUserId(ownerUserId)
                .inviteeEmail("partner@test.com")
                .inviteeUserId(partnerUserid)
                .proposedRole(AccessLevel.READ_WRITE)
                .proposedSharingRule(SharingRule.MUTUAL_DEFAULT)
                .build());

        // Step 3: Accept the invite
        SharedSpace sharedSpaceWithParticipants = sharedSpaceService.acceptInvitation(AcceptInvitationCommand.builder()
                .invitationId(invitation.id())
                .acceptingUserId(partnerUserid)
                .build());

        sharedSpaceService.addResource(spaceId, AddSharedResourceCommand.builder()
                .type(ResourceType.TRANSACTION)
                .itemIds(List.of("INCOME", "EXPENSES"))
                .ownerUserId(partnerUserid)
                .sharedByOwner(true)
                .build());

//        sharedSpaceService.
        List<SharedResource> visibleResources = sharedSpaceService.getVisibleResources(sharedSpaceWithParticipants.id(), ownerUserId);

        System.out.println();
    }
}
