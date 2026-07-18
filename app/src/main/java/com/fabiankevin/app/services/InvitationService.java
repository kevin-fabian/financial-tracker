package com.fabiankevin.app.services;

import com.fabiankevin.app.models.party.InvitationSummary;
import com.fabiankevin.app.services.commands.party.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.party.invitations.SendInvitationCommand;

import java.util.List;
import java.util.UUID;

public interface InvitationService {
    InvitationSummary sendInvitation(SendInvitationCommand command);

    InvitationSummary acceptInvitation(AcceptInvitationCommand command);

    InvitationSummary rejectInvitation(RejectInvitationCommand command);

    List<InvitationSummary> getInvitationsByUserId(UUID userId);
}
