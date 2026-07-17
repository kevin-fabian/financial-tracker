package com.fabiankevin.app.services;

import com.fabiankevin.app.models.party.Invitation;
import com.fabiankevin.app.models.party.InvitationSummary;
import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.services.commands.party.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.SendInvitationCommand;

import java.util.List;
import java.util.UUID;

public interface InvitationService {
    Invitation sendInvitation(SendInvitationCommand command);

    Party acceptInvitation(AcceptInvitationCommand command);

    Invitation rejectInvitation(RejectInvitationCommand command);

    List<InvitationSummary> getInvitationsByUserId(UUID userId);
}
