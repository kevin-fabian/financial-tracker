package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.persistence.entities.HouseholdEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaHouseholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultHouseholdRepository implements HouseholdRepository {
    private final JpaHouseholdRepository jpaHouseholdRepository;

    @Override
    public Household save(Household household) {
        HouseholdEntity saved = jpaHouseholdRepository.save(HouseholdEntity.from(household));
        return saved.toModel();
    }

    @Override
    public Optional<Household> findById(UUID id) {
        return jpaHouseholdRepository.findById(id).map(HouseholdEntity::toModel);
    }

    @Override
    public Optional<Household> findByUserId(UUID userId) {
        return jpaHouseholdRepository.findByUserId(userId).stream()
                .findFirst()
                .map(HouseholdEntity::toModel);
    }

    @Override
    public List<Household> retrieveByUserId(UUID userId) {
        return jpaHouseholdRepository.findByUserId(userId).stream()
                .map(HouseholdEntity::toModel)
                .toList();
    }

    @Override
    public List<Household> findAllById(Iterable<UUID> ids) {
        return jpaHouseholdRepository.findAllById(ids).stream()
                .map(HouseholdEntity::toModel)
                .toList();
    }

    @Override
    public List<UUID> findMembersUserIdsByUserId(UUID userId) {
        return jpaHouseholdRepository.findHouseholdMemberUserIdsByLeaderId(userId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaHouseholdRepository.deleteById(id);
    }
}
