package com.fabiankevin.app.services;

import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.models.shared_space.InvitationSummary;
import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.services.commands.shared_space.invitations.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.invitations.RejectInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.invitations.SendInvitationCommand;

import java.util.List;
import java.util.UUID;

public interface InvitationService {
    Invitation sendInvitation(SendInvitationCommand command);

    Party acceptInvitation(AcceptInvitationCommand command);

    Invitation rejectInvitation(RejectInvitationCommand command);

    List<InvitationSummary> getInvitationsByUserId(UUID userId);
}
