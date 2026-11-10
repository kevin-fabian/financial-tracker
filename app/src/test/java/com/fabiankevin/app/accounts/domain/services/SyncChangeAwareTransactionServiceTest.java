package com.fabiankevin.app.accounts.domain.services;

import com.fabiankevin.app.accounts.domain.models.Account;
import com.fabiankevin.app.accounts.domain.models.AccountType;
import com.fabiankevin.app.categories.domain.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.SummarySeries;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.SyncChangeAwareTransactionService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.services.queries.SummaryQuery;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import com.fabiankevin.app.web.controllers.dtos.transactions.TransactionResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncChangeAwareTransactionServiceTest {

    private final TransactionService delegate = mock(TransactionService.class);
    private final SyncChangeService syncChangeService = mock(SyncChangeService.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final SyncChangeAwareTransactionService service = new SyncChangeAwareTransactionService(
            delegate, syncChangeService, jsonMapper
    );

    @Nested
    class AddTransaction {

        @Test
        void givenNewTransaction_shouldDelegateAndRegisterCreateChange() {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            AddTransactionCommand command = AddTransactionCommand.builder()
                    .id(transactionId)
                    .amount(150.0)
                    .description("Lunch")
                    .transactionDate(LocalDate.of(2025, 6, 1))
                    .categoryId(categoryId)
                    .accountId(accountId)
                    .userId(userId)
                    .build();

            Account account = Account.builder()
                    .id(accountId)
                    .name("Cash")
                    .currency(java.util.Currency.getInstance("USD"))
                    .type(AccountType.CASH)
                    .active(true)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Food")
                    .type(TransactionType.EXPENSE)
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .account(account)
                    .type(TransactionType.EXPENSE)
                    .category(category)
                    .amount(150.0)
                    .description("Lunch")
                    .transactionDate(LocalDate.of(2025, 6, 1))
                    .createdAt(Instant.parse("2025-06-01T12:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T12:00:00Z"))
                    .addedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .build();

            when(delegate.addTransaction(command)).thenReturn(transaction);

            Transaction result = service.addTransaction(command);

            assertThat(result).isSameAs(transaction);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.TRANSACTIONS);
            assertThat(change.entityId()).isEqualTo(transactionId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.CREATE);
            assertThat(change.payload()).isNotNull();
        }

        @Test
        void givenTransactionWithSmallAmount_shouldRegisterChangeWithCorrectPayload() {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            AddTransactionCommand command = AddTransactionCommand.builder()
                    .id(transactionId)
                    .amount(1.50)
                    .description("Coffee")
                    .transactionDate(LocalDate.of(2025, 6, 1))
                    .categoryId(categoryId)
                    .accountId(accountId)
                    .userId(userId)
                    .build();

            Account account = Account.builder()
                    .id(accountId)
                    .name("Cash")
                    .currency(java.util.Currency.getInstance("USD"))
                    .type(AccountType.CASH)
                    .active(true)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .user(User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Food")
                    .type(TransactionType.EXPENSE)
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .account(account)
                    .type(TransactionType.EXPENSE)
                    .category(category)
                    .amount(1.50)
                    .description("Coffee")
                    .transactionDate(LocalDate.of(2025, 6, 1))
                    .createdAt(Instant.parse("2025-06-01T08:00:00Z"))
                    .updatedAt(Instant.parse("2025-06-01T08:00:00Z"))
                    .addedBy(User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                    .updatedBy(User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                    .build();

            when(delegate.addTransaction(command)).thenReturn(transaction);

            Transaction result = service.addTransaction(command);

            assertThat(result).isSameAs(transaction);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            String payload = captor.getValue().payload();
            assertThat(payload).contains("\"description\":\"Coffee\"");
            assertThat(payload).contains("\"amount\":1.5");
        }
    }

    @Nested
    class PatchTransaction {

        @Test
        void givenExistingTransaction_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            PatchTransactionCommand command = PatchTransactionCommand.builder()
                    .id(transactionId)
                    .accountId(accountId)
                    .description("Updated description")
                    .categoryId(categoryId)
                    .amount(200.0)
                    .transactionDate(LocalDate.of(2025, 7, 1))
                    .userId(userId)
                    .build();

            Account account = Account.builder()
                    .id(accountId)
                    .name("BDO")
                    .currency(java.util.Currency.getInstance("PHP"))
                    .type(AccountType.BANK_ACCOUNT)
                    .active(true)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .build();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("Groceries")
                    .type(TransactionType.EXPENSE)
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .build();

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .account(account)
                    .type(TransactionType.EXPENSE)
                    .category(category)
                    .amount(200.0)
                    .description("Updated description")
                    .transactionDate(LocalDate.of(2025, 7, 1))
                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-07-01T10:00:00Z"))
                    .addedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .updatedBy(User.builder().id(userId).firstName("John").lastName("Doe").build())
                    .build();

            when(delegate.patchTransaction(command)).thenReturn(transaction);

            Transaction result = service.patchTransaction(command);

            assertThat(result).isSameAs(transaction);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.TRANSACTIONS);
            assertThat(change.entityId()).isEqualTo(transactionId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.UPDATE);
            assertThat(change.payload()).isNotNull();
        }

        @Test
        void givenTransactionWithUpdatedFields_shouldRegisterChangeWithUpdatedPayload() {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            PatchTransactionCommand command = PatchTransactionCommand.builder()
                    .id(transactionId)
                    .accountId(accountId)
                    .description("Entertainment expense")
                    .categoryId(categoryId)
                    .amount(500.0)
                    .transactionDate(LocalDate.of(2025, 8, 15))
                    .userId(userId)
                    .build();

            Account account = Account.builder()
                    .id(accountId)
                    .name("GCash")
                    .currency(java.util.Currency.getInstance("PHP"))
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
                    .active(true)
                    .system(false)
                    .userId(userId)
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .build();

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .account(account)
                    .type(TransactionType.EXPENSE)
                    .category(category)
                    .amount(500.0)
                    .description("Entertainment expense")
                    .transactionDate(LocalDate.of(2025, 8, 15))
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2025-08-15T14:00:00Z"))
                    .addedBy(User.builder().id(userId).firstName("Alice").lastName("Wonder").build())
                    .updatedBy(User.builder().id(userId).firstName("Alice").lastName("Wonder").build())
                    .build();

            when(delegate.patchTransaction(command)).thenReturn(transaction);

            Transaction result = service.patchTransaction(command);

            assertThat(result).isSameAs(transaction);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            String payload = captor.getValue().payload();
            assertThat(payload).contains("\"description\":\"Entertainment expense\"");
            assertThat(payload).contains("\"amount\":500.0");
        }
    }

    @Nested
    class NonMutatingMethods {

        @Test
        void addTransactions_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();
            List<AddTransactionCommand> commands = List.of(
                    AddTransactionCommand.builder()
                            .id(UUID.randomUUID())
                            .amount(100.0)
                            .description("Test")
                            .transactionDate(LocalDate.now())
                            .categoryId(UUID.randomUUID())
                            .accountId(UUID.randomUUID())
                            .userId(userId)
                            .build()
            );

            service.addTransactions(commands);

            verify(delegate, times(1)).addTransactions(commands);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void deleteAllByUserId_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();
            when(delegate.deleteAllByUserId(userId)).thenReturn(5);

            int result = service.deleteAllByUserId(userId);

            assertThat(result).isEqualTo(5);
            verify(delegate, times(1)).deleteAllByUserId(userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void getSummary_shouldDelegateWithoutRegisteringChange() {
            SummaryQuery query = SummaryQuery.builder()
                    .userIds(Set.of(UUID.randomUUID()))
                    .type(com.fabiankevin.app.models.enums.SummaryType.MONTHLY)
                    .from(LocalDate.of(2025, 1, 1))
                    .to(LocalDate.of(2025, 1, 31))
                    .build();

            when(delegate.getSummary(query)).thenReturn(new SummarySeries(com.fabiankevin.app.models.enums.SummaryType.MONTHLY, List.of()));

            service.getSummary(query);

            verify(delegate, times(1)).getSummary(query);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void getTransactionsByPageQuery_shouldDelegateWithoutRegisteringChange() {
            UUID userId = UUID.randomUUID();
            PageQuery query = PageQuery.builder().page(0).size(10).build();

            when(delegate.getTransactionsByPageQuery(query, userId, TransactionType.EXPENSE))
                    .thenReturn(new Page<>(List.of(), 0, 10, 0, 0, true, true));

            service.getTransactionsByPageQuery(query, userId, TransactionType.EXPENSE);

            verify(delegate, times(1)).getTransactionsByPageQuery(query, userId, TransactionType.EXPENSE);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void deleteTransaction_shouldDelegateWithoutRegisteringChange() {
            UUID transactionId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            service.deleteTransaction(transactionId, userId);

            verify(delegate, times(1)).deleteTransaction(transactionId, userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void getTransactionById_shouldDelegateWithoutRegisteringChange() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            TransactionResponse expected = TransactionResponse.builder()
                    .id(id)
                    .amount(new com.fabiankevin.app.web.controllers.dtos.AmountResponse(100.0, "USD"))
                    .description("Test")
                    .transactionDate(LocalDate.of(2025, 1, 1))
                    .build();

            when(delegate.getTransactionById(id, userId)).thenReturn(expected);

            TransactionResponse result = service.getTransactionById(id, userId);

            assertThat(result).isSameAs(expected);
            verify(delegate, times(1)).getTransactionById(id, userId);
            verify(syncChangeService, never()).create(any());
        }
    }
}
