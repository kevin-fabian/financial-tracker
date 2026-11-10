package com.fabiankevin.app.services;

import com.fabiankevin.app.categories.domain.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncChangeAwareBudgetServiceTest {

    private final BudgetService delegate = mock(BudgetService.class);
    private final SyncChangeService syncChangeService = mock(SyncChangeService.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final SyncChangeAwareBudgetService service = new SyncChangeAwareBudgetService(
            delegate, syncChangeService, jsonMapper
    );

    @Nested
    class CreateBudget {

        @Test
        void givenNewBudget_shouldDelegateAndRegisterCreateChange() {
            UUID userId = UUID.randomUUID();
            UUID budgetId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(5000.0)
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Food")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            Budget budget = Budget.builder()
                    .id(budgetId)
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(5000.0)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            BudgetSummary expected = BudgetSummary.builder()
                    .budget(budget)
                    .spent(1200.50)
                    .spentPercentage(24.01)
                    .build();

            when(delegate.createBudget(command)).thenReturn(expected);

            BudgetSummary result = service.createBudget(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.BUDGETS);
            assertThat(change.entityId()).isEqualTo(budgetId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.CREATE);
            Budget capturedBudget = jsonMapper.readValue(change.payload(), Budget.class);
            assertThat(capturedBudget).isEqualTo(expected.budget());
        }

        @Test
        void givenBudgetWithZeroSpent_shouldRegisterChangeWithCorrectPayload() {
            UUID userId = UUID.randomUUID();
            UUID budgetId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(10000.0)
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Savings")
                    .type(TransactionType.INCOME)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            Budget budget = Budget.builder()
                    .id(budgetId)
                    .user(User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                    .updatedBy(User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(10000.0)
                    .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            BudgetSummary expected = BudgetSummary.builder()
                    .budget(budget)
                    .spent(0.0)
                    .spentPercentage(0.0)
                    .build();

            when(delegate.createBudget(command)).thenReturn(expected);

            BudgetSummary result = service.createBudget(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            Budget capturedBudget = jsonMapper.readValue(captor.getValue().payload(), Budget.class);
            assertThat(capturedBudget).isEqualTo(expected.budget());
        }
    }

    @Nested
    class PatchBudget {

        @Test
        void givenExistingBudget_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID budgetId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            PatchBudgetCommand command = PatchBudgetCommand.builder()
                    .id(budgetId)
                    .categoryId(categoryId)
                    .period(BudgetPeriod.MONTHLY)
                    .allocated(7500.0)
                    .userId(userId)
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Groceries")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            Budget budget = Budget.builder()
                    .id(budgetId)
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(7500.0)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            BudgetSummary expected = BudgetSummary.builder()
                    .budget(budget)
                    .spent(3200.75)
                    .spentPercentage(42.68)
                    .build();

            when(delegate.patchBudget(command)).thenReturn(expected);

            BudgetSummary result = service.patchBudget(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.BUDGETS);
            assertThat(change.entityId()).isEqualTo(budgetId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.UPDATE);
            Budget capturedBudget = jsonMapper.readValue(captor.getValue().payload(), Budget.class);
            assertThat(capturedBudget).isEqualTo(expected.budget());
        }

        @Test
        void givenBudgetWithUpdatedFields_shouldRegisterChangeWithUpdatedPayload() {
            UUID userId = UUID.randomUUID();
            UUID budgetId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            PatchBudgetCommand command = PatchBudgetCommand.builder()
                    .id(budgetId)
                    .categoryId(categoryId)
                    .period(BudgetPeriod.MONTHLY)
                    .allocated(15000.0)
                    .userId(userId)
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Entertainment")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .build();

            Budget budget = Budget.builder()
                    .id(budgetId)
                    .user(User.builder().id(userId).firstName("Alice").lastName("Wonder").build())
                    .updatedBy(User.builder().id(userId).firstName("Alice").lastName("Wonder").build())
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(15000.0)
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            BudgetSummary expected = BudgetSummary.builder()
                    .budget(budget)
                    .spent(450.25)
                    .spentPercentage(3.0)
                    .build();

            when(delegate.patchBudget(command)).thenReturn(expected);

            BudgetSummary result = service.patchBudget(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            Budget capturedBudget = jsonMapper.readValue(captor.getValue().payload(), Budget.class);
            assertThat(capturedBudget).isEqualTo(expected.budget());
        }
    }

    @Nested
    class NonMutatingMethods {

        @Test
        void getBudgetSummaryByIdAndUserId_shouldDelegateWithoutRegisteringChange() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Food")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            Budget budget = Budget.builder()
                    .id(id)
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(5000.0)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            BudgetSummary expected = BudgetSummary.builder()
                    .budget(budget)
                    .spent(100.0)
                    .spentPercentage(2.0)
                    .build();

            when(delegate.getBudgetSummaryByIdAndUserId(id, userId)).thenReturn(expected);

            BudgetSummary result = service.getBudgetSummaryByIdAndUserId(id, userId);

            assertThat(result).isSameAs(expected);
            verify(delegate, times(1)).getBudgetSummaryByIdAndUserId(id, userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void getBudgetsByUserId_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Food")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            Budget budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(5000.0)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            BudgetSummary summary = BudgetSummary.builder()
                    .budget(budget)
                    .spent(100.0)
                    .spentPercentage(2.0)
                    .build();

            when(delegate.getBudgetsByUserId(userId)).thenReturn(List.of(summary));

            List<BudgetSummary> result = service.getBudgetsByUserId(userId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isSameAs(summary);
            verify(delegate, times(1)).getBudgetsByUserId(userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void deleteBudgetById_shouldDelegateWithoutRegisteringChange() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            service.deleteBudgetById(id, userId);

            verify(delegate, times(1)).deleteBudgetById(id, userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void deleteAllByUserId_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();

            service.deleteAllByUserId(userId);

            verify(delegate, times(1)).deleteAllByUserId(userId);
            verify(syncChangeService, never()).create(any());
        }
    }
}
