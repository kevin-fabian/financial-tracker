package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.BudgetAlreadyExistException;
import com.fabiankevin.app.exceptions.BudgetNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.persistence.BudgetRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultBudgetService implements BudgetService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final UserClient userClient;

    @Transactional
    @Override
    public BudgetSummary createBudget(CreateBudgetCommand command) {
        Instant now = Instant.now();
        ZonedDateTime monthStart = now.atZone(ZoneOffset.UTC).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC);
        if (budgetRepository.existsByCategoryIdAndUserIdAndCreatedAtBetween(
                command.categoryId(), command.userId(), monthStart.toInstant(), monthStart.plusMonths(1).toInstant())) {
            throw new BudgetAlreadyExistException("A budget already exists for this category this month");
        }

        Category category = categoryService.getCategoryById(command.categoryId(), command.userId());
        Budget budget = Budget.builder()
                .userId(command.userId())
                .lastUpdatedBy(command.userId())
                .period(command.period())
                .category(category)
                .allocated(command.allocated())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Budget saved = budgetRepository.save(budget);
        double spent = transactionRepository.sumSpentByCategoryIdAndUserId(saved.category().id(), saved.userId());
        return toSummary(saved, spent);
    }

    private BudgetSummary toSummary(Budget budget, double spent) {
        Optional<User> user = userClient.getUsersByIds(List.of(budget.userId())).stream().findFirst();
        String lastUpdatedByName = user.map(User::fullName).orElse(null);
        String firstName = user.map(User::firstName).orElse(null);
        String lastName = user.map(User::lastName).orElse(null);
        String initial = user.map(User::initial).orElse(null);
        double allocated = budget.allocated();
        double spentPercentage = allocated > 0 ? (spent / allocated) * 100.0 : 0.0;

        return BudgetSummary.builder()
                .id(budget.id())
                .userId(budget.userId())
                .lastUpdatedBy(budget.lastUpdatedBy())
                .lastUpdatedByName(lastUpdatedByName)
                .firstName(firstName)
                .lastName(lastName)
                .initial(initial)
                .updatedAt(budget.updatedAt())
                .createdAt(budget.createdAt())
                .period(budget.period())
                .categoryId(budget.category().id())
                .categoryName(budget.category().name())
                .categoryIcon(budget.category().icon())
                .members(List.of())
                .allocated(allocated)
                .spent(spent)
                .spentPercentage(spentPercentage)
                .build();
    }

    @Override
    public List<BudgetSummary> getBudgetsByUserId(UUID userId) {
        Instant now = Instant.now();
        ZonedDateTime monthStart = now.atZone(ZoneOffset.UTC).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC);
        List<BudgetSummary> summaries = budgetRepository.findAllBudgetSummaryByUserId(
                List.of(userId), monthStart.toInstant(), monthStart.plusMonths(1).toInstant());
        return enrichWithLastUpdatedByName(summaries);
    }

    @Transactional
    @Override
    public List<BudgetSummary> recreateBudgetsFromLastMonth(UUID userId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Instant lastMonthStart = now.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant currentMonthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Budget> lastMonthBudgets = budgetRepository.findAllByUserIdAndCreatedAtBetween(userId, lastMonthStart, currentMonthStart);

        return lastMonthBudgets.stream()
                .map(budget -> {
                    CreateBudgetCommand command = CreateBudgetCommand.builder()
                            .userId(userId)
                            .categoryId(budget.category().id())
                            .period(budget.period())
                            .allocated(budget.allocated())
                            .build();
                    return createBudget(command);
                })
                .toList();
    }

    private List<BudgetSummary> enrichWithLastUpdatedByName(List<BudgetSummary> summaries) {
        Set<UUID> lastUpdatedByIds = summaries.stream()
                .map(BudgetSummary::lastUpdatedBy)
                .collect(Collectors.toSet());

        Map<UUID, User> usersById = lastUpdatedByIds.isEmpty()
                ? Map.of()
                : userClient.getUsersByIds(new ArrayList<>(lastUpdatedByIds)).stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        return summaries.stream()
                .map(summary -> {
                    Optional<User> user = Optional.ofNullable(usersById.get(summary.lastUpdatedBy()));
                    String name = user.map(User::fullName).orElse(null);
                    String firstName = user.map(User::firstName).orElse(null);
                    String lastName = user.map(User::lastName).orElse(null);
                    String initial = user.map(User::initial).orElse(null);
                    return summary.toBuilder()
                            .lastUpdatedByName(name)
                            .firstName(firstName)
                            .lastName(lastName)
                            .initial(initial)
                            .build();
                })
                .toList();
    }

    @Transactional
    @Override
    public BudgetSummary patchBudget(PatchBudgetCommand command) {
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

        Budget saved = budgetRepository.save(builder.build());
        double spent = transactionRepository.sumSpentByCategoryIdAndUserId(saved.category().id(), saved.userId());
        return toSummary(saved, spent);
    }

    @Transactional
    @Override
    public void deleteBudgetById(UUID id, UUID userId) {
        budgetRepository.deleteByIdAndUserId(id, userId);
    }
}
