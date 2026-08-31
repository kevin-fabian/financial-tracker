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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
        if (budgetRepository.existsByCategoryIdAndUserId(
                command.categoryId(), command.userId())) {
            throw new BudgetAlreadyExistException("A budget already exists for this category this month");
        }
        Instant now = Instant.now();

        Category category = categoryService.getCategoryById(command.categoryId(), command.userId());
        User user = User.of(command.userId());
        Budget budget = Budget.builder()
                .user(user)
                .updatedBy(user)
                .period(command.period())
                .category(category)
                .allocated(command.allocated())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Budget saved = budgetRepository.save(budget);
        double spent = transactionRepository.sumSpentByCategoryIdAndUserId(saved.category().id(), saved.user().id());
        return toSummary(saved, spent);
    }

    private BudgetSummary toSummary(Budget budget, double spent) {
        double allocated = budget.allocated();
        double spentPercentage = allocated > 0 ? (spent / allocated) * 100.0 : 0.0;

        return BudgetSummary.builder()
                .budget(budget)
                .spent(spent)
                .spentPercentage(spentPercentage)
                .build();
    }

    @Override
    public List<BudgetSummary> getBudgetsByUserId(UUID userId) {
        LocalDate monthStart = ZonedDateTime.now(ZoneOffset.UTC).withDayOfMonth(1).toLocalDate();
        LocalDate monthEnd = monthStart.plusMonths(1);
        List<BudgetSummary> summaries = budgetRepository.findAllBudgetSummaryByUserId(
                List.of(userId), monthStart, monthEnd);
        return enrichWithUserData(summaries);
    }

    private List<BudgetSummary> enrichWithUserData(List<BudgetSummary> summaries) {
        List<UUID> userIds = summaries.stream()
                .map(summary -> summary.budget().user().id())
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return summaries;
        }

        Map<UUID, User> usersById = userClient.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(User::id, u -> u));

        return summaries.stream()
                .map(summary -> {
                    UUID userId = summary.budget().user().id();
                    User enrichedUser = usersById.get(userId);
                    if (enrichedUser == null) {
                        return summary;
                    }
                    Budget enrichedBudget = summary.budget().toBuilder()
                            .user(User.builder()
                                    .id(enrichedUser.id())
                                    .firstName(enrichedUser.firstName())
                                    .lastName(enrichedUser.lastName())
                                    .build())
                            .updatedBy(User.builder()
                                    .id(enrichedUser.id())
                                    .firstName(enrichedUser.firstName())
                                    .lastName(enrichedUser.lastName())
                                    .build())
                            .build();
                    return BudgetSummary.builder()
                            .budget(enrichedBudget)
                            .spent(summary.spent())
                            .spentPercentage(summary.spentPercentage())
                            .build();
                })
                .toList();
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

    @Transactional
    @Override
    public BudgetSummary patchBudget(PatchBudgetCommand command) {
        UUID id = command.id();
        UUID userId = command.userId();

        Budget existing = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(BudgetNotFoundException::new);

        User user = User.of(userId);
        Budget.BudgetBuilder builder = existing.toBuilder()
                .updatedBy(user)
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
        double spent = transactionRepository.sumSpentByCategoryIdAndUserId(saved.category().id(), saved.user().id());
        return toSummary(saved, spent);
    }

    @Transactional
    @Override
    public void deleteBudgetById(UUID id, UUID userId) {
        budgetRepository.deleteByIdAndUserId(id, userId);
    }
}
