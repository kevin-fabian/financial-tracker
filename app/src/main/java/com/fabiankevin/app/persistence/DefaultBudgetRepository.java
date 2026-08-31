package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.persistence.entities.BudgetEntity;
import com.fabiankevin.app.persistence.entities.projections.BudgetSummaryProjection;
import com.fabiankevin.app.persistence.jpa_repositories.JpaBudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    @Override
    public Optional<Budget> findByIdAndUserId(UUID id, UUID userId) {
        return jpaBudgetRepository.findByIdAndUserId(id, userId).map(BudgetEntity::toModel);
    }

    @Override
    public boolean existsByCategoryIdAndUserId(UUID categoryId, UUID userId) {
        return jpaBudgetRepository.existsByCategoryIdAndUserId(categoryId, userId);
    }

    @Override
    public boolean existsByCategoryIdAndUserIdAndCreatedAtBetween(UUID categoryId, UUID userId, Instant startInclusive, Instant endExclusive) {
        return jpaBudgetRepository.existsByCategoryIdAndUserIdAndCreatedAtBetween(categoryId, userId, startInclusive, endExclusive);
    }

    @Override
    public int deleteByIdAndUserId(UUID id, UUID userId) {
        return jpaBudgetRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public List<Budget> findAllByUserIdAndCreatedAtBetween(UUID userId, Instant startInclusive, Instant endExclusive) {
        return jpaBudgetRepository.findAllByUserIdAndCreatedAtBetween(userId, startInclusive, endExclusive)
                .stream()
                .map(BudgetEntity::toModel)
                .toList();
    }

    @Override
    public List<BudgetSummary> findAllBudgetSummaryByUserId(List<UUID> usersId, LocalDate startMonth, LocalDate endMonth) {
        return jpaBudgetRepository.findAllBudgetSummaryByUserIds(usersId, startMonth, endMonth)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private BudgetSummary toSummary(BudgetSummaryProjection projection) {
        double spent = projection.spent();
        double allocated = projection.allocated();
        double spentPercentage = allocated > 0 ? (spent / allocated) * 100.0 : 0.0;
        UUID userId = projection.userId();
        UUID lastUpdatedBy = projection.updatedBy();

        return BudgetSummary.builder()
                .id(projection.id())
                .userId(userId)
                .user(User.of(userId))
                .updatedBy(User.of(lastUpdatedBy))
                .updatedAt(projection.updatedAt())
                .createdAt(projection.createdAt())
                .period(BudgetPeriod.valueOf(projection.period()))
                .allocated(allocated)
                .categoryId(projection.category().getId())
                .categoryName(projection.category().getName())
                .categoryIcon(projection.category().getIcon())
                .spent(spent)
                .spentPercentage(spentPercentage)
                .build();
    }
}
