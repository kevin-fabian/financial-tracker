package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.party.Party;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartyRepository {
    Party save(Party space);

    Optional<Party> findById(UUID id);

    Optional<Party> findByUserId(UUID userId);

    List<Party> retrieveByUserId(UUID userId);

    List<Party> findAllById(Iterable<UUID> ids);

    List<UUID> findParticipantUserIdsByUserId(UUID userId);

    void deleteById(UUID id);
}
