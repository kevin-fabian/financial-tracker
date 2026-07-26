package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.BudgetAlreadyExistException;
import com.fabiankevin.app.exceptions.BudgetNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultBudgetService implements BudgetService {
    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final UserClient userClient;

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
        List<BudgetSummary> summaries = budgetRepository.findAllBudgetSummaryByUserId(List.of(userId));
        return enrichWithLastUpdatedByName(summaries);
    }

    private List<BudgetSummary> enrichWithLastUpdatedByName(List<BudgetSummary> summaries) {
        List<UUID> lastUpdatedByIds = summaries.stream()
                .map(BudgetSummary::lastUpdatedBy)
                .distinct()
                .toList();

        Map<UUID, User> usersById = lastUpdatedByIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(lastUpdatedByIds).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        return summaries.stream()
                .map(summary -> {
                    User user = usersById.get(summary.lastUpdatedBy());
                    String name = user != null ? user.fullName() : null;
                    return summary.toBuilder().lastUpdatedByName(name).build();
                })
                .toList();
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

    @Transactional
    @Override
    public void deleteBudgetById(UUID id, UUID userId) {
        budgetRepository.deleteByIdAndUserId(id, userId);
    }
}
