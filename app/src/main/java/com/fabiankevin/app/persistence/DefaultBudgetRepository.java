package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.persistence.entities.BudgetEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaBudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DefaultBudgetRepository implements BudgetRepository {
    private final JpaBudgetRepository jpaBudgetRepository;

    @Override
    public Budget save(Budget budget) {
        return jpaBudgetRepository.save(BudgetEntity.from(budget)).toModel();
    }

    @Override
    public Optional<Budget> findById(UUID id) {
        return jpaBudgetRepository.findById(id).map(BudgetEntity::toModel);
    }
}
