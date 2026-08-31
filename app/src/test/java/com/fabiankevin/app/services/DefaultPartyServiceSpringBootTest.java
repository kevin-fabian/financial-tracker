package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.party.ForbiddenException;
import com.fabiankevin.app.exceptions.party.InviterCannotAcceptOwnInvitationException;
import com.fabiankevin.app.exceptions.party.PartyMemberAlreadyExistsException;
import com.fabiankevin.app.models.*;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.enums.party.InvitationStatus;
import com.fabiankevin.app.models.enums.party.SharingMode;
import com.fabiankevin.app.models.party.Invitation;
import com.fabiankevin.app.models.party.InvitationSummary;
import com.fabiankevin.app.models.party.PartySummary;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.InvitationRepository;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.party.OrganizePartyCommand;
import com.fabiankevin.app.services.commands.party.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.SendInvitationCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
public class DefaultPartyServiceSpringBootTest {
    @Autowired
    private PartyService partyService;
    @Autowired
    private InvitationService invitationService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private StatsService statsService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private InvitationRepository invitationRepository;
    @MockitoBean
    private UserClient userClient;
    @MockitoBean
    private RestClient restClient;
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @DisplayName("""
            Mutual sharing mode: transactions should be combined after space acceptance
            """)
    @Test
    void mutualSharingFlow_shouldCombineTransactions() {
        UUID ownerUserId = UUID.randomUUID();
        UUID partnerUserid = UUID.randomUUID();
        String partnerEmail = "partner@example.com";

        // Owner transactions
        addTransaction(ownerUserId, 500);
        addTransaction(ownerUserId, 1500);
        addTransaction(ownerUserId, 3000);

        // Partner transactions
        addTransaction(partnerUserid, 500);
        addTransaction(partnerUserid, 1500);
        addTransaction(partnerUserid, 3000);

        // Step 1: Create a space
        PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                .partyName("Partner Space")
                .partyLeaderId(ownerUserId)
                .sharingMode(SharingMode.EVEN_SHARE)
                .build());
        UUID spaceId = initialParty.id();

        // Step 2: Invite a partner in a space
        when(userClient.getUserByEmail(partnerEmail))
                .thenReturn(User.builder().id(partnerUserid).firstName("Partner").lastName("User").build());
        InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                .partyId(spaceId)
                .inviterPlayerId(ownerUserId)
                .inviteeEmail(partnerEmail)
                .build());

        // Step 3: Accept the invite
        invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                .invitationId(invitation.id())
                .acceptingPlayerId(partnerUserid)
                .build());

        Page<Transaction> transactionsByPageQuery = transactionService.getTransactionsByPageQuery(PageQuery.withDefaults(),
                partnerUserid, null);
        List<Transaction> sharedTransactions = transactionsByPageQuery.content();

        assertEquals(6, sharedTransactions.size(), "combined transaction count");
    }

    @DisplayName("""
            Mutual sharing mode: stats should combine total balance, expenses, and income after space acceptance
            """)
    @Test
    void mutualSharingFlow_shouldCombineStats() {
        UUID ownerUserId = UUID.randomUUID();
        UUID partnerUserid = UUID.randomUUID();
        String partnerEmail = "partner@example.com";

        // Owner: income 10000, expenses 3000
        addIncomeTransaction(ownerUserId, 10000);
        addTransaction(ownerUserId, 1000);
        addTransaction(ownerUserId, 2000);

        // Partner: income 6000, expenses 2000
        addIncomeTransaction(partnerUserid, 6000);
        addTransaction(partnerUserid, 500);
        addTransaction(partnerUserid, 1500);

        // Step 1: Create a space
        PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                .partyName("Partner Space")
                .partyLeaderId(ownerUserId)
                .sharingMode(SharingMode.EVEN_SHARE)
                .build());
        UUID spaceId = initialParty.id();

        // Step 2: Invite a partner in a space
        when(userClient.getUserByEmail(partnerEmail))
                .thenReturn(User.builder().id(partnerUserid).firstName("Partner").lastName("User").build());
        InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                .partyId(spaceId)
                .inviterPlayerId(ownerUserId)
                .inviteeEmail(partnerEmail)
                .build());

        // Step 3: Accept the invite
        invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                .invitationId(invitation.id())
                .acceptingPlayerId(partnerUserid)
                .build());

        StatsQuery query = StatsQuery.builder()
                .fromDate(LocalDate.now().withDayOfMonth(1))
                .toDate(LocalDate.now())
                .build();

        var summary = statsService.getStatsSummary(ownerUserId, query);

        assertEquals(16000.0, summary.totalIncome(), 0.001, "combined income");
        assertEquals(5000.0, summary.totalExpenses(), 0.001, "combined expenses");
        assertEquals(11000.0, summary.totalBalance(), 0.001, "combined balance");
    }

    @Nested
    class Party {
        @DisplayName("""
            Party leader disbands the party: party should no longer be returned for the leader
            """)
        @Test
        void partyLeaderDisbandParty_thenPartyShouldBeDisbanded() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserid = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserid).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserid)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserid).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());

            // Step 3: Partner accepts the invitation
            invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                    .invitationId(invitation.id())
                    .acceptingPlayerId(partnerUserid)
                    .build());

            PartySummary afterAcceptance = partyService.retrieveByUserId(ownerUserId).getFirst();
            assertEquals(2, afterAcceptance.partyMembers().size(),
                    "party should have leader + member after acceptance");

            // Step 4: Leader disbands the party
            partyService.disbandParty(partyId, ownerUserId);

            assertTrue(partyService.retrieveByUserId(ownerUserId).isEmpty(),
                    "leader should have no parties after disbanding");
        }

        @DisplayName("""
            Party leader kicks a member: invitation flow followed by kick removes the member from the party
            """)
        @Test
        void partyLeaderKicksPartyMember_thenPartyMemberShouldBeKicked() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserid = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserid).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserid)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserid).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());

            assertEquals(1, partyService.retrieveByUserId(ownerUserId).getFirst().partyMembers().size(),
                    "party should start with only the leader after invitation is sent");

            // Step 3: Partner accepts the invitation
            invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                    .invitationId(invitation.id())
                    .acceptingPlayerId(partnerUserid)
                    .build());

            PartySummary afterAcceptance = partyService.retrieveByUserId(ownerUserId).getFirst();
            assertEquals(2, afterAcceptance.partyMembers().size(),
                    "party should have leader + member after acceptance");

            // Step 4: Leader kicks the partner
            partyService.kickPartyMember(partyId, partnerUserid, ownerUserId);

            PartySummary afterKick = partyService.retrieveByUserId(ownerUserId).getFirst();
            assertEquals(1, afterKick.partyMembers().size(),
                    "party should only contain the leader after the member is kicked");
            assertTrue(afterKick.partyMembers().getFirst().partyLeader(),
                    "remaining member should be the leader");
        }

        @DisplayName("""
            Party member tries to kick the leader: should throw
            """)
        @Test
        void partyMemberKicksLeader_thenThrows() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserid = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserid).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserid)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserid).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());

            // Step 3: Partner accepts the invitation
            invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                    .invitationId(invitation.id())
                    .acceptingPlayerId(partnerUserid)
                    .build());

            PartySummary afterAcceptance = partyService.retrieveByUserId(ownerUserId).getFirst();
            assertEquals(2, afterAcceptance.partyMembers().size(),
                    "party should have leader + member after acceptance");

            // Step 4: Member tries to kick the leader
            assertThrows(ForbiddenException.class, () -> partyService.kickPartyMember(partyId, ownerUserId, partnerUserid));
        }

        @DisplayName("""
            User with incoming invitations creates a party: all incoming invitations should be cancelled
            """)
        @Test
        void userWithIncomingInvitationsCreatesParty_thenIncomingInvitationsShouldBeCancelled() {
            UUID leader1Id = UUID.randomUUID();
            UUID leader2Id = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();
            String recipientEmail = "recipient@example.com";

            // Step 1: Two leaders create parties and both invite the recipient
            PartySummary leader1Party = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Leader 1 Party")
                    .partyLeaderId(leader1Id)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());

            PartySummary leader2Party = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Leader 2 Party")
                    .partyLeaderId(leader2Id)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());

            when(userClient.getUserByEmail(recipientEmail))
                    .thenReturn(User.builder().id(recipientId).firstName("Recipient").lastName("User").build());
            when(userClient.getUsersByIds(List.of(leader1Id, recipientId)))
                    .thenReturn(List.of(
                            User.builder().id(leader1Id).firstName("Leader").lastName("One").build(),
                            User.builder().id(recipientId).firstName("Recipient").lastName("User").build()
                    ));
            when(userClient.getUsersByIds(List.of(leader2Id, recipientId)))
                    .thenReturn(List.of(
                            User.builder().id(leader2Id).firstName("Leader").lastName("Two").build(),
                            User.builder().id(recipientId).firstName("Recipient").lastName("User").build()
                    ));

            invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(leader1Party.id())
                    .inviterPlayerId(leader1Id)
                    .inviteeEmail(recipientEmail)
                    .build());
            invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(leader2Party.id())
                    .inviterPlayerId(leader2Id)
                    .inviteeEmail(recipientEmail)
                    .build());

            List<Invitation> incomingBefore = invitationRepository.findByInviteeUserId(recipientId);
            assertEquals(2, incomingBefore.size(), "recipient should have 2 incoming invitations");
            assertTrue(incomingBefore.stream().allMatch(i -> i.status() == InvitationStatus.PENDING),
                    "all incoming invitations should be pending before party creation");

            // Step 2: Recipient creates their own party
            partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Recipient Party")
                    .partyLeaderId(recipientId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());

            List<Invitation> incomingAfter = invitationRepository.findByInviteeUserId(recipientId);
            assertTrue(incomingAfter.stream().allMatch(i -> i.status() == InvitationStatus.CANCELLED),
                    "all incoming invitations should be cancelled after the recipient creates a party");
        }
    }

    @Nested
    class PartyInvitation {
        @DisplayName("""
            Party leader creates a party, invites a party member who accepts: party should have 2 members (leader + member)
            """)
        @Test
        void partyLeaderInvitesMemberWhoAccepts_thenPartyShouldHaveTwoMembers() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserId = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserId).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserId)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserId).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());

            // Step 3: Partner accepts the invitation
            invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                    .invitationId(invitation.id())
                    .acceptingPlayerId(partnerUserId)
                    .build());

            PartySummary afterAcceptance = partyService.retrieveByUserId(ownerUserId).getFirst();
            assertEquals(2, afterAcceptance.partyMembers().size(),
                    "party should have leader + member after acceptance");
        }

        @DisplayName("""
            Party leader creates a party, invites a party leader who already belongs to a party: should throw
            """)
        @Test
        void partyLeaderInvitesExistingPartyLeader_thenShouldThrow() {
            UUID leader1Id = UUID.randomUUID();
            UUID leader2Id = UUID.randomUUID();
            String leader2Email = "leader2@example.com";

            // Step 1: Leader 1 creates a party
            PartySummary leader1Party = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Leader 1 Party")
                    .partyLeaderId(leader1Id)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID party1Id = leader1Party.id();

            // Step 2: Leader 2 creates a party
            partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Leader 2 Party")
                    .partyLeaderId(leader2Id)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());

            // Step 3: Leader 1 tries to invite leader 2 who already belongs to a party
            when(userClient.getUserByEmail(leader2Email))
                    .thenReturn(User.builder().id(leader2Id).firstName("Leader").lastName("Two").build());

            assertThrows(PartyMemberAlreadyExistsException.class, () ->
                    invitationService.sendInvitation(SendInvitationCommand.builder()
                            .partyId(party1Id)
                            .inviterPlayerId(leader1Id)
                            .inviteeEmail(leader2Email)
                            .build()));
        }

        @DisplayName("""
            Party leader creates a party, invites a party member who rejects: party should contain only the leader
            """)
        @Test
        void partyLeaderInvitesMemberWhoRejects_thenPartyShouldHaveLeaderOnly() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserId = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserId).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserId)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserId).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());

            // Step 3: Partner rejects the invitation
            invitationService.rejectInvitation(new RejectInvitationCommand(invitation.id(), partnerUserId));

            PartySummary afterRejection = partyService.retrieveByUserId(ownerUserId).getFirst();
            assertEquals(1, afterRejection.partyMembers().size(),
                    "party should contain only the leader after rejection");
            assertTrue(afterRejection.partyMembers().getFirst().partyLeader(),
                    "remaining member should be the leader");
        }

        @DisplayName("""
            Party leader invites a party member, then leader cancels the invitation: invitation should be cancelled
            """)
        @Test
        void partyLeaderCancelsInvitation_thenInvitationShouldBeCancelled() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserId = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserId).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserId)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserId).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());

            assertTrue(invitationRepository.findById(invitation.id()).isPresent(),
                    "invitation should exist after being sent");

            // Step 3: Leader cancels the invitation
            invitationService.rejectInvitation(new RejectInvitationCommand(invitation.id(), ownerUserId));

            Invitation cancelledInvitation = invitationRepository.findById(invitation.id()).orElseThrow();
            assertEquals(InvitationStatus.CANCELLED, cancelledInvitation.status(),
                    "invitation should have cancelled status after leader cancels it");
        }

        @DisplayName("""
            A party member leaves from an existing party: user should not belong to any party
            """)
        @Test
        void partyMemberLeavesExistingParty_thenUserShouldNotBelongToAnyParty() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserId = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner and accept
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserId).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserId)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserId).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());
            invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                    .invitationId(invitation.id())
                    .acceptingPlayerId(partnerUserId)
                    .build());

            assertEquals(2, partyService.retrieveByUserId(ownerUserId).getFirst().partyMembers().size(),
                    "party should have leader + member after acceptance");

            // Step 3: Party member leaves the party
            partyService.kickPartyMember(partyId, partnerUserId, partnerUserId);

            assertTrue(partyService.retrieveByUserId(partnerUserId).isEmpty(),
                    "leaving user should not belong to any party");
        }

        @DisplayName("""
            Invitee rejects invitation: party should still contain only the leader
            """)
        @Test
        void inviteeRejectsInvitation_shouldKeepPartyWithLeaderOnly() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserid = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserid).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserid)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserid).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());

            assertEquals(1, partyService.retrieveByUserId(ownerUserId).getFirst().partyMembers().size(),
                    "party should start with only the leader after invitation is sent");

            // Step 3: Partner rejects the invitation
            invitationService.rejectInvitation(new RejectInvitationCommand(invitation.id(), partnerUserid));

            PartySummary afterRejection = partyService.retrieveByUserId(ownerUserId).getFirst();
            assertEquals(1, afterRejection.partyMembers().size(),
                    "party should still contain only the leader after rejection");
            assertTrue(afterRejection.partyMembers().getFirst().partyLeader(),
                    "remaining member should be the leader");
        }

        @DisplayName("""
            Party leader invites two party members who both accept: party should have 3 members
            """)
        @Test
        void partyLeaderInvitesTwoMembersWhoAccept_thenPartyShouldHaveThreeMembers() {
            UUID ownerUserId = UUID.randomUUID();
            UUID firstMemberId = UUID.randomUUID();
            UUID secondMemberId = UUID.randomUUID();
            String firstMemberEmail = "first@example.com";
            String secondMemberEmail = "second@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite first member and accept
            when(userClient.getUserByEmail(firstMemberEmail))
                    .thenReturn(User.builder().id(firstMemberId).firstName("First").lastName("Member").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, firstMemberId)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(firstMemberId).firstName("First").lastName("Member").build()
                    ));
            InvitationSummary firstInvitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(firstMemberEmail)
                    .build());
            invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                    .invitationId(firstInvitation.id())
                    .acceptingPlayerId(firstMemberId)
                    .build());

            // Step 3: Invite second member and accept
            when(userClient.getUserByEmail(secondMemberEmail))
                    .thenReturn(User.builder().id(secondMemberId).firstName("Second").lastName("Member").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, secondMemberId)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(secondMemberId).firstName("Second").lastName("Member").build()
                    ));
            InvitationSummary secondInvitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(secondMemberEmail)
                    .build());
            invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                    .invitationId(secondInvitation.id())
                    .acceptingPlayerId(secondMemberId)
                    .build());

            PartySummary afterAcceptance = partyService.retrieveByUserId(ownerUserId).getFirst();
            assertEquals(3, afterAcceptance.partyMembers().size(),
                    "party should have leader + two members after both accept");
        }

        @DisplayName("""
            Party leader creates a party, invites a party member who is already in a party: should throw
            """)
        @Test
        void partyLeaderInvitesMemberAlreadyInParty_thenShouldThrow() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserId = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner and accept
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserId).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserId)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserId).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());
            invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                    .invitationId(invitation.id())
                    .acceptingPlayerId(partnerUserId)
                    .build());

            // Step 3: Try to invite the same partner again who is already a member
            assertThrows(PartyMemberAlreadyExistsException.class, () ->
                    invitationService.sendInvitation(SendInvitationCommand.builder()
                            .partyId(partyId)
                            .inviterPlayerId(ownerUserId)
                            .inviteeEmail(partnerEmail)
                            .build()));
        }

        @DisplayName("""
            Party leader creates a party, invites himself: should throw
            """)
        @Test
        void partyLeaderInvitesHimself_thenShouldThrow() {
            UUID ownerUserId = UUID.randomUUID();
            String ownerEmail = "owner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Solo Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Leader tries to invite himself
            when(userClient.getUserByEmail(ownerEmail))
                    .thenReturn(User.builder().id(ownerUserId).firstName("Owner").lastName("User").build());

            assertThrows(PartyMemberAlreadyExistsException.class, () ->
                    invitationService.sendInvitation(SendInvitationCommand.builder()
                            .partyId(partyId)
                            .inviterPlayerId(ownerUserId)
                            .inviteeEmail(ownerEmail)
                            .build()));
        }

        @DisplayName("""
            Party leader creates a party, invites a party member, then the leader tries to accept: should throw
            """)
        @Test
        void partyLeaderTriesToAcceptOwnInvitation_thenShouldThrow() {
            UUID ownerUserId = UUID.randomUUID();
            UUID partnerUserId = UUID.randomUUID();
            String partnerEmail = "partner@example.com";

            // Step 1: Create a party as the leader
            PartySummary initialParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Partner Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID partyId = initialParty.id();

            // Step 2: Invite a partner
            when(userClient.getUserByEmail(partnerEmail))
                    .thenReturn(User.builder().id(partnerUserId).firstName("Partner").lastName("User").build());
            when(userClient.getUsersByIds(List.of(ownerUserId, partnerUserId)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build(),
                            User.builder().id(partnerUserId).firstName("Partner").lastName("User").build()
                    ));
            InvitationSummary invitation = invitationService.sendInvitation(SendInvitationCommand.builder()
                    .partyId(partyId)
                    .inviterPlayerId(ownerUserId)
                    .inviteeEmail(partnerEmail)
                    .build());

            // Step 3: Leader tries to accept their own invitation
            assertThrows(InviterCannotAcceptOwnInvitationException.class, () ->
                    invitationService.acceptInvitation(AcceptInvitationCommand.builder()
                            .invitationId(invitation.id())
                            .acceptingPlayerId(ownerUserId)
                            .build()));
        }

        @DisplayName("""
            Party leader creates a party, then creates a party again: should return the existing party
            """)
        @Test
        void partyLeaderCreatesPartyAgain_thenShouldReturnExistingParty() {
            UUID ownerUserId = UUID.randomUUID();

            // Step 1: Create a party as the leader
            PartySummary firstParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("My Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());
            UUID firstPartyId = firstParty.id();

            when(userClient.getUsersByIds(List.of(ownerUserId)))
                    .thenReturn(List.of(
                            User.builder().id(ownerUserId).firstName("Owner").lastName("User").build()
                    ));

            // Step 2: Create a party again
            PartySummary secondParty = partyService.organize(OrganizePartyCommand.builder()
                    .partyName("Another Party")
                    .partyLeaderId(ownerUserId)
                    .sharingMode(SharingMode.EVEN_SHARE)
                    .build());

            assertEquals(firstPartyId, secondParty.id(),
                    "second organize call should return the existing party");
            assertEquals("My Party", secondParty.name(),
                    "second organize call should return the original party name");
        }
    }


    private void addTransaction(UUID userId, double amount) {
        Account account = createOrGetAccount(userId);
        Category category = createOrGetCategory(userId);

        transactionService.addTransaction(AddTransactionCommand.builder()
                .amount(amount)
                .description("Ramen")
                .userId(userId)
                .accountId(account.id())
                .categoryId(category.id())
                .transactionDate(LocalDate.now())
                .build());
    }

    private void addIncomeTransaction(UUID userId, double amount) {
        Account account = createOrGetAccount(userId);
        Category category = createOrGetIncomeCategory(userId);

        transactionService.addTransaction(AddTransactionCommand.builder()
                .amount(amount)
                .description("Salary")
                .userId(userId)
                .accountId(account.id())
                .categoryId(category.id())
                .transactionDate(LocalDate.now())
                .build());
    }

    private Category createOrGetIncomeCategory(UUID userId) {
        List<Category> categories = categoryRepository.findAllByNamesIn(List.of("Salary")).stream()
                .filter(c -> c.userId().equals(userId))
                .toList();
        if (categories.isEmpty()) {
            return categoryRepository.save(Category.builder()
                    .type(TransactionType.INCOME)
                    .name("Salary")
                    .userId(userId)
                    .icon("salary")
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        return categories.getFirst();
    }

    private Category createOrGetCategory(UUID userId) {
        List<Category> allByNamesIn = categoryRepository.findAllByNamesIn(List.of("Food & Drinks")).stream()
                .filter(acc -> acc.userId().equals(userId))
                .toList();
        if (allByNamesIn.isEmpty()) {
            return categoryRepository.save(Category.builder()
                    .type(TransactionType.EXPENSE)
                    .name("Food & Drinks")
                    .userId(userId)
                    .icon("food")
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        return allByNamesIn.getFirst();
    }

    private Account createOrGetAccount(UUID userId) {
        List<Account> cashWallet = accountRepository.findAllByNamesIn(List.of("Cash Wallet"))
                .stream()
                .filter(acc -> acc.userId().equals(userId))
                .toList();

        if (cashWallet.isEmpty()) {
            return accountRepository.save(Account.builder()
                    .name("Cash Wallet")
                    .type(AccountType.CASH)
                    .userId(userId)
                    .currency(Currency.getInstance("PHP"))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        return cashWallet.getFirst();
    }

}
