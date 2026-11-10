package com.fabiankevin.app.services;

import com.fabiankevin.app.accounts.domain.models.Account;
import com.fabiankevin.app.accounts.domain.models.AccountType;
import com.fabiankevin.app.categories.domain.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import com.fabiankevin.app.services.recurring_transactions.commands.CreateRecurringTransactionCommand;
import com.fabiankevin.app.services.recurring_transactions.commands.UpdateRecurringTransactionCommand;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncChangeAwareRecurringTransactionServiceTest {

    private final RecurringTransactionService delegate = mock(RecurringTransactionService.class);
    private final SyncChangeService syncChangeService = mock(SyncChangeService.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final SyncChangeAwareRecurringTransactionService service = new SyncChangeAwareRecurringTransactionService(
            delegate, syncChangeService, jsonMapper
    );

    @Nested
    class CreateRecurringTransaction {

        @Test
        void givenNewRecurringTransaction_shouldDelegateAndRegisterCreateChange() {
            UUID userId = UUID.randomUUID();
            UUID recurringTransactionId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            CreateRecurringTransactionCommand command = CreateRecurringTransactionCommand.builder()
                    .userId(userId)
                    .description("Netflix")
                    .amount(15.99)
                    .variableAmount(false)
                    .categoryId(categoryId)
                    .accountId(accountId)
                    .noEndDate(true)
                    .dayOfMonth(1)
                    .durationMonths(0)
                    .build();

            Account account = Account.builder()
                    .id(accountId)
                    .name("BDO")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.BANK_ACCOUNT)
                    .active(true)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Entertainment")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            RecurringTransactionSummary expected = RecurringTransactionSummary.builder()
                    .id(recurringTransactionId)
                    .description("Netflix")
                    .amount(15.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(1)
                    .nextOccurrenceDate(LocalDate.of(2025, 2, 1))
                    .endDate(null)
                    .remainingDays(31)
                    .transactionStatus(TransactionStatus.UPCOMING)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            when(delegate.create(command)).thenReturn(expected);

            RecurringTransactionSummary result = service.create(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.RECURRING_TRANSACTIONS);
            assertThat(change.entityId()).isEqualTo(recurringTransactionId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.CREATE);
            assertThat(change.payload()).isNotNull();
        }

        @Test
        void givenRecurringTransactionWithVariableAmount_shouldRegisterChangeWithCorrectPayload() {
            UUID userId = UUID.randomUUID();
            UUID recurringTransactionId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            CreateRecurringTransactionCommand command = CreateRecurringTransactionCommand.builder()
                    .userId(userId)
                    .description("Utilities")
                    .amount(0)
                    .variableAmount(true)
                    .categoryId(categoryId)
                    .accountId(accountId)
                    .noEndDate(false)
                    .dayOfMonth(15)
                    .durationMonths(6)
                    .build();

            Account account = Account.builder()
                    .id(accountId)
                    .name("GCash")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .active(true)
                    .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .user(User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Utilities")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            RecurringTransactionSummary expected = RecurringTransactionSummary.builder()
                    .id(recurringTransactionId)
                    .description("Utilities")
                    .amount(0)
                    .variableAmount(true)
                    .category(category)
                    .account(account)
                    .dayOfMonth(15)
                    .nextOccurrenceDate(LocalDate.of(2025, 7, 15))
                    .endDate(LocalDate.of(2025, 12, 1))
                    .remainingDays(44)
                    .transactionStatus(TransactionStatus.UPCOMING)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .updatedBy(User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                    .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            when(delegate.create(command)).thenReturn(expected);

            RecurringTransactionSummary result = service.create(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            String payload = captor.getValue().payload();
            assertThat(payload).contains("\"description\":\"Utilities\"");
            assertThat(payload).contains("\"amount\":0.0");
            assertThat(payload).contains("\"variableAmount\":true");
        }
    }

    @Nested
    class UpdateRecurringTransaction {

        @Test
        void givenExistingRecurringTransaction_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID recurringTransactionId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UpdateRecurringTransactionCommand command = UpdateRecurringTransactionCommand.builder()
                    .userId(userId)
                    .id(recurringTransactionId)
                    .description("Netflix Premium")
                    .amount(19.99)
                    .variableAmount(false)
                    .categoryId(categoryId)
                    .accountId(accountId)
                    .noEndDate(true)
                    .dayOfMonth(1)
                    .durationMonths(0)
                    .build();

            Account account = Account.builder()
                    .id(accountId)
                    .name("BDO")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.BANK_ACCOUNT)
                    .active(true)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Entertainment")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            RecurringTransactionSummary expected = RecurringTransactionSummary.builder()
                    .id(recurringTransactionId)
                    .description("Netflix Premium")
                    .amount(19.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(1)
                    .nextOccurrenceDate(LocalDate.of(2025, 2, 1))
                    .endDate(null)
                    .remainingDays(31)
                    .transactionStatus(TransactionStatus.UPCOMING)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            when(delegate.updateRecurringTransaction(command)).thenReturn(expected);

            RecurringTransactionSummary result = service.updateRecurringTransaction(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.RECURRING_TRANSACTIONS);
            assertThat(change.entityId()).isEqualTo(recurringTransactionId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.UPDATE);
            assertThat(change.payload()).isNotNull();
        }

        @Test
        void givenRecurringTransactionWithUpdatedFields_shouldRegisterChangeWithUpdatedPayload() {
            UUID userId = UUID.randomUUID();
            UUID recurringTransactionId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UpdateRecurringTransactionCommand command = UpdateRecurringTransactionCommand.builder()
                    .userId(userId)
                    .id(recurringTransactionId)
                    .description("Spotify Family")
                    .amount(9.99)
                    .variableAmount(false)
                    .categoryId(categoryId)
                    .accountId(accountId)
                    .noEndDate(false)
                    .dayOfMonth(20)
                    .durationMonths(12)
                    .build();

            Account account = Account.builder()
                    .id(accountId)
                    .name("GCash")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.E_WALLET)
                    .active(true)
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .user(User.builder().id(userId).firstName("Alice").lastName("Wonder").build())
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

            RecurringTransactionSummary expected = RecurringTransactionSummary.builder()
                    .id(recurringTransactionId)
                    .description("Spotify Family")
                    .amount(9.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(20)
                    .nextOccurrenceDate(LocalDate.of(2025, 12, 20))
                    .endDate(LocalDate.of(2027, 11, 10))
                    .remainingDays(375)
                    .transactionStatus(TransactionStatus.UPCOMING)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .updatedBy(User.builder().id(userId).firstName("Alice").lastName("Wonder").build())
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                    .build();

            when(delegate.updateRecurringTransaction(command)).thenReturn(expected);

            RecurringTransactionSummary result = service.updateRecurringTransaction(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            String payload = captor.getValue().payload();
            assertThat(payload).contains("\"description\":\"Spotify Family\"");
            assertThat(payload).contains("\"amount\":9.99");
        }
    }

    @Nested
    class NonMutatingMethods {

        @Test
        void getRecurringTransactionsByUserId_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Account account = Account.builder()
                    .id(accountId)
                    .name("BDO")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.BANK_ACCOUNT)
                    .active(true)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
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

            RecurringTransactionSummary summary = RecurringTransactionSummary.builder()
                    .id(UUID.randomUUID())
                    .description("Netflix")
                    .amount(15.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(1)
                    .nextOccurrenceDate(LocalDate.of(2025, 2, 1))
                    .endDate(null)
                    .remainingDays(31)
                    .transactionStatus(TransactionStatus.UPCOMING)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            when(delegate.getRecurringTransactionsByUserId(userId)).thenReturn(List.of(summary));

            List<RecurringTransactionSummary> result = service.getRecurringTransactionsByUserId(userId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isSameAs(summary);
            verify(delegate, times(1)).getRecurringTransactionsByUserId(userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void deleteRecurringTransactionById_shouldDelegateAndRegisterDeleteChange() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Account account = Account.builder()
                    .id(accountId)
                    .name("BDO")
                    .currency(Currency.getInstance("PHP"))
                    .type(AccountType.BANK_ACCOUNT)
                    .active(true)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Entertainment")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .active(true)
                    .system(false)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            RecurringTransactionSummary existing = RecurringTransactionSummary.builder()
                    .id(id)
                    .description("Netflix")
                    .amount(15.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(1)
                    .nextOccurrenceDate(LocalDate.of(2025, 2, 1))
                    .endDate(null)
                    .remainingDays(31)
                    .transactionStatus(TransactionStatus.UPCOMING)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            when(delegate.getRecurringTransactionsByUserId(userId)).thenReturn(List.of(existing));

            service.deleteRecurringTransactionById(id, userId);

            verify(delegate, times(1)).getRecurringTransactionsByUserId(userId);
            verify(delegate, times(1)).deleteRecurringTransactionById(id, userId);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.RECURRING_TRANSACTIONS);
            assertThat(change.entityId()).isEqualTo(id);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.DELETE);
            assertThat(change.payload()).isNotNull();
        }

        @Test
        void deleteAllByUserId_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();

            service.deleteAllByUserId(userId);

            verify(delegate, times(1)).deleteAllByUserId(userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void processDueRecurringTransactions_shouldDelegateWithoutRegisteringChange() {
            service.processDueRecurringTransactions();

            verify(delegate, times(1)).processDueRecurringTransactions();
            verify(syncChangeService, never()).create(any());
        }
    }
}
