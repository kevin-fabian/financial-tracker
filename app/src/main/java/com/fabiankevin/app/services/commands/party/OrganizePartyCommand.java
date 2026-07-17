package com.fabiankevin.app.services.commands.party;

import com.fabiankevin.app.models.enums.party.SharingMode;
import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

@Builder
public record OrganizePartyCommand(
        UUID partyLeaderId,
        String partyName,
        SharingMode sharingMode
) {
    public OrganizePartyCommand {
        Objects.requireNonNull(partyLeaderId, "Party leader ID cannot be null");
        Objects.requireNonNull(sharingMode, "Sharing mode cannot be null");
    }
}
