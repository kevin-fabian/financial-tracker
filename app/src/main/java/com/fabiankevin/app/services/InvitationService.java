package com.fabiankevin.app.services;

import com.fabiankevin.app.models.household.InvitationSummary;
import com.fabiankevin.app.services.commands.household.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.household.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.household.invitations.SendInvitationCommand;

import java.util.List;
import java.util.UUID;

public interface InvitationService {
    InvitationSummary sendInvitation(SendInvitationCommand command);

    InvitationSummary acceptInvitation(AcceptInvitationCommand command);

    InvitationSummary rejectInvitation(RejectInvitationCommand command);

    List<InvitationSummary> getInvitationsByUserId(UUID userId);
}
