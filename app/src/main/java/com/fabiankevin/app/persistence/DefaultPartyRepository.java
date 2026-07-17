package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.persistence.entities.PartyEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultPartyRepository implements PartyRepository {
    private final JpaPartyRepository jpaPartyRepository;

    @Override
    public Party save(Party space) {
        PartyEntity saved = jpaPartyRepository.save(PartyEntity.from(space));
        return saved.toModel();
    }

    @Override
    public Optional<Party> findById(UUID id) {
        return jpaPartyRepository.findById(id).map(PartyEntity::toModel);
    }

    @Override
    public Optional<Party> findByUserId(UUID userId) {
        return jpaPartyRepository.findByPlayerId(userId).stream()
                .findFirst()
                .map(PartyEntity::toModel);
    }

    @Override
    public List<Party> retrieveByUserId(UUID userId) {
        return jpaPartyRepository.findByPlayerId(userId).stream()
                .map(PartyEntity::toModel)
                .toList();
    }

    @Override
    public List<Party> findAllById(Iterable<UUID> ids) {
        return jpaPartyRepository.findAllById(ids).stream()
                .map(PartyEntity::toModel)
                .toList();
    }

    @Override
    public List<UUID> findParticipantUserIdsByUserId(UUID userId) {
        return jpaPartyRepository.findPartyMemberPlayerIdsByPlayerId(userId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaPartyRepository.deleteById(id);
    }
}
