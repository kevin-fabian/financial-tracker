package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.party.Party;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartyRepository {
    Party save(Party space);

    Optional<Party> findById(UUID id);

    Optional<Party> findByPlayerId(UUID userId);

    List<Party> retrieveByPlayerId(UUID userId);

    List<Party> findAllById(Iterable<UUID> ids);

    List<UUID> findPartyMembersPlayerIdsByPlayerId(UUID userId);

    void deleteById(UUID id);
}
