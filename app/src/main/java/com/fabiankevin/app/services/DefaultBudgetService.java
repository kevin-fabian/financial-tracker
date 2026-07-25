package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.persistence.BudgetRepository;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DefaultBudgetService implements BudgetService {
    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;

    @Transactional
    @Override
    public Budget createBudget(CreateBudgetCommand command) {
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
}
