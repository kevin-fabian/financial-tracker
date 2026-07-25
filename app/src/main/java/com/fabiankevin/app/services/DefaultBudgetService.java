package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.BudgetAlreadyExistException;
import com.fabiankevin.app.exceptions.BudgetNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.persistence.BudgetRepository;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultBudgetService implements BudgetService {
    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;

    @Transactional
    @Override
    public Budget createBudget(CreateBudgetCommand command) {
        if (budgetRepository.existsByCategoryIdAndUserId(command.categoryId(), command.userId())) {
            throw new BudgetAlreadyExistException("A budget already exists for this category");
        }

        Category category = categoryService.getCategoryById(command.categoryId(), command.userId());

        Instant now = Instant.now();
        Budget budget = Budget.builder()
                .userId(command.userId())
                .lastUpdatedBy(command.userId())
                .period(command.period())
                .category(category)
                .allocated(command.allocated())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return budgetRepository.save(budget);
    }

    @Override
    public List<BudgetSummary> getBudgetsByUserId(UUID userId) {
        return budgetRepository.findAllBudgetSummaryByUserId(List.of(userId));
    }

    @Transactional
    @Override
    public Budget patchBudget(PatchBudgetCommand command) {
        UUID id = command.id();
        UUID userId = command.userId();

        Budget existing = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(BudgetNotFoundException::new);

        Budget.BudgetBuilder builder = existing.toBuilder()
                .lastUpdatedBy(userId)
                .updatedAt(Instant.now());

        Optional.ofNullable(command.categoryId())
                .ifPresent(categoryId -> {
                    if (budgetRepository.existsByCategoryIdAndUserId(categoryId, userId)) {
                        throw new BudgetAlreadyExistException("A budget already exists for this category");
                    }
                    builder.category(categoryService.getCategoryById(categoryId, userId));
                });
        Optional.ofNullable(command.period())
                .ifPresent(builder::period);
        Optional.ofNullable(command.allocated())
                .ifPresent(builder::allocated);

        return budgetRepository.save(builder.build());
    }

    @Override
    public void deleteBudgetById(UUID id, UUID userId) {
        budgetRepository.deleteByIdAndUserId(id, userId);
    }
}
