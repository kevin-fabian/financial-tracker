package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.household.Household;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseholdRepository {
    Household save(Household space);

    Optional<Household> findById(UUID id);

    Optional<Household> findByUserId(UUID userId);

    List<Household> retrieveByUserId(UUID userId);

    List<Household> findAllById(Iterable<UUID> ids);

    List<UUID> findMembersUserIdsByUserId(UUID userId);

    void deleteById(UUID id);
}
