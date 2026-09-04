package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.exceptions.InvalidAmountException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.RecurringTransactionRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.recurring_transactions.commands.CreateRecurringTransactionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DefaultRecurringTransactionService service;

    private Account account;
    private Category category;
    private CreateRecurringTransactionCommand command;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        account = Account.builder()
                .id(UUID.randomUUID())
                .name("Checking")
                .user(User.of(userId))
                .currency(Currency.getInstance("USD"))
                .type(AccountType.CASH)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        category = Category.builder()
                .id(UUID.randomUUID())
                .name("GROCERIES")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon("local_grocery_store")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        command = CreateRecurringTransactionCommand.builder()
                .userId(userId)
                .description("Monthly subscription")
                .amount(15.99)
                .variableAmount(false)
                .categoryId(category.id())
                .accountId(account.id())
                .noEndDate(false)
                .dayOfMonth(15)
                .durationMonths(6)
                .build();
    }

    @Nested
    class Create {

        @Test
        void givenValidCommand_thenReturnsSummaryWithAllFields() {
            when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));
            when(categoryRepository.findById(category.id())).thenReturn(Optional.of(category));
            when(recurringTransactionRepository.save(any())).thenAnswer(invocation -> {
                RecurringTransaction input = invocation.getArgument(0);
                return input.toBuilder().id(UUID.randomUUID()).createdAt(Instant.now()).updatedAt(Instant.now()).build();
            });
            when(userClient.getUsersByIds(List.of(command.userId()))).thenReturn(List.of(
                    User.builder().id(command.userId()).firstName("John").lastName("Doe").build()
            ));

            LocalDate today = LocalDate.now();
            RecurringTransactionSummary summary = service.create(command);

            LocalDate expectedEndDate = today.plusMonths(command.durationMonths());

            assertNotNull(summary, "summary should not be null");
            assertNotNull(summary.id(), "id should be generated");
            assertEquals("Monthly subscription", summary.description(), "description should match command");
            assertEquals(15.99, summary.amount(), "amount should match command");
            assertFalse(summary.variableAmount(), "variableAmount should be false");
            assertEquals(15, summary.dayOfMonth(), "dayOfMonth should match command");
            LocalDate expectedNextOccurrenceDate = today.getDayOfMonth() < command.dayOfMonth()
                    ? today.withDayOfMonth(command.dayOfMonth())
                    : today.plusMonths(1).withDayOfMonth(command.dayOfMonth());

            assertEquals(TransactionStatus.UPCOMING, summary.transactionStatus(), "status should be UPCOMING when nextOccurrenceDate is in the future");
            assertEquals(RecurringTransactionStatus.ACTIVE, summary.status(), "recurring status should be ACTIVE");
            assertNotNull(summary.category(), "category should not be null");
            assertEquals(category.id(), summary.category().id(), "category should match");
            assertNotNull(summary.account(), "account should not be null");
            assertEquals(account.id(), summary.account().id(), "account should match");
            assertNotNull(summary.createdAt(), "createdAt should not be null");
            assertNotNull(summary.updatedAt(), "updatedAt should not be null");
            assertEquals(expectedEndDate, summary.endDate(), "endDate should be now plus durationMonths");
            assertEquals(expectedNextOccurrenceDate, summary.nextOccurrenceDate(), "nextOccurrenceDate should be derived from dayOfMonth");
            assertNotNull(summary.updatedBy(), "user should not be null");
            assertEquals("John", summary.updatedBy().firstName(), "user firstName should match");
            assertEquals("Doe", summary.updatedBy().lastName(), "user lastName should match");
            assertEquals("JD", summary.updatedBy().initial(), "user initial should match");
            int expectedRemainingDays = (int) ChronoUnit.DAYS.between(today, summary.nextOccurrenceDate());
            assertEquals(expectedRemainingDays, summary.remainingDays(), "remainingDays should be computed from nextOccurrenceDate");

            ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringTransactionRepository).save(captor.capture());
            RecurringTransaction saved = captor.getValue();
            assertEquals(command.userId(), saved.updatedById(), "saved user should match command");
            assertEquals(RecurringTransactionStatus.ACTIVE, saved.status(), "saved status should be ACTIVE");
        }

        @Test
        void givenNonExistentAccount_thenThrowsAccountNotFoundException() {
            when(accountRepository.findById(account.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(command))
                    .as("should throw AccountNotFoundException when account does not exist")
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository).findById(account.id());
            verify(categoryRepository, never()).findById(any());
            verify(recurringTransactionRepository, never()).save(any());
        }

        @ParameterizedTest
        @ValueSource(ints = {-12, 50})
        void givenDayOfMonthOutOfRange_thenThrowsIllegalArgumentException(int dayOfMonth) {
            CreateRecurringTransactionCommand invalidCommand = command.toBuilder().dayOfMonth(dayOfMonth).build();

            assertThatThrownBy(() -> service.create(invalidCommand))
                    .as("should throw IllegalArgumentException when dayOfMonth is out of range")
                    .isInstanceOf(IllegalArgumentException.class);

            verify(accountRepository, never()).findById(any());
            verify(categoryRepository, never()).findById(any());
            verify(recurringTransactionRepository, never()).save(any());
        }

        @Test
        void givenNoEndDateFlag_thenEndDateIsNullAndDayOfMonthZeroDoesNotThrow() {
            CreateRecurringTransactionCommand noEndDateCommand = command.toBuilder()
                    .noEndDate(true)
                    .dayOfMonth(0)
                    .build();

            when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));
            when(categoryRepository.findById(category.id())).thenReturn(Optional.of(category));
            when(recurringTransactionRepository.save(any())).thenAnswer(invocation -> {
                RecurringTransaction input = invocation.getArgument(0);
                return input.toBuilder().id(UUID.randomUUID()).createdAt(Instant.now()).updatedAt(Instant.now()).build();
            });
            lenient().when(userClient.getUsersByIds(List.of(noEndDateCommand.userId()))).thenReturn(List.of(
                    User.builder().id(noEndDateCommand.userId()).firstName("Kevin").lastName("Fabian").build()
            ));

            RecurringTransactionSummary summary = service.create(noEndDateCommand);

            assertNotNull(summary, "summary should not be null");
            assertNotNull(summary.id(), "id should be generated");
            assertEquals("Monthly subscription", summary.description(), "description should match command");
            assertEquals(15.99, summary.amount(), "amount should match command");
            assertEquals(RecurringTransactionStatus.ACTIVE, summary.status(), "recurring status should be ACTIVE");
            assertNotNull(summary.category(), "category should not be null");
            assertEquals(category.id(), summary.category().id(), "category should match");
            assertNotNull(summary.account(), "account should not be null");
            assertEquals(account.id(), summary.account().id(), "account should match");
            assertNotNull(summary.createdAt(), "createdAt should not be null");
            assertNotNull(summary.updatedAt(), "updatedAt should not be null");
            assertNotNull(summary.nextOccurrenceDate(), "nextOccurrenceDate should not be null");
            assertEquals(TransactionStatus.UPCOMING, summary.transactionStatus(), "transactionStatus should be UPCOMING");
            assertNull(summary.endDate(), "endDate should be null when noEndDate is true");

            verify(accountRepository).findById(account.id());
            verify(categoryRepository).findById(category.id());
            verify(recurringTransactionRepository).save(any());
        }

        @Test
        void givenVariableAmountFlag_thenAmountIsZero() {
            CreateRecurringTransactionCommand variableAmountCommand = command.toBuilder()
                    .variableAmount(true)
                    .amount(0)
                    .build();

            when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));
            when(categoryRepository.findById(category.id())).thenReturn(Optional.of(category));
            when(recurringTransactionRepository.save(any())).thenAnswer(invocation -> {
                RecurringTransaction input = invocation.getArgument(0);
                return input.toBuilder().id(UUID.randomUUID()).createdAt(Instant.now()).updatedAt(Instant.now()).build();
            });
            lenient().when(userClient.getUsersByIds(List.of(variableAmountCommand.userId()))).thenReturn(List.of(
                    User.builder().id(variableAmountCommand.userId()).firstName("Kevin").lastName("Fabian").build()
            ));

            RecurringTransactionSummary summary = service.create(variableAmountCommand);

            assertNotNull(summary, "summary should not be null");
            assertTrue(summary.variableAmount(), "variableAmount should be true");
            assertEquals(0.0, summary.amount(), "amount should be zero when variableAmount is true");
            assertEquals(RecurringTransactionStatus.ACTIVE, summary.status(), "recurring status should be ACTIVE");
            assertNotNull(summary.category(), "category should not be null");
            assertNotNull(summary.account(), "account should not be null");

            ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringTransactionRepository).save(captor.capture());
            RecurringTransaction saved = captor.getValue();
            assertTrue(saved.variableAmount(), "saved variableAmount should be true");
            assertEquals(0.0, saved.amount(), "saved amount should be zero when variableAmount is true");
        }

        @Test
        void givenVariableAmountFlagWithNonZeroAmount_thenThrowsInvalidAmountException() {
            CreateRecurringTransactionCommand invalidCommand = command.toBuilder()
                    .variableAmount(true)
                    .amount(15.99)
                    .build();

            assertThatThrownBy(() -> service.create(invalidCommand))
                    .as("should throw InvalidAmountException when variableAmount is true and amount is non-zero")
                    .isInstanceOf(InvalidAmountException.class);

            verify(accountRepository, never()).findById(any());
            verify(categoryRepository, never()).findById(any());
            verify(recurringTransactionRepository, never()).save(any());
        }
    }

    @Nested
    class ProcessDueRecurringTransactions {

        @Test
        void givenDueRecurringTransactions_thenCreatesTransactionsWithRecurringTransactionId() {
            UUID userId = account.user().id();
            UUID recurringId1 = UUID.randomUUID();
            UUID recurringId2 = UUID.randomUUID();

            RecurringTransaction recurring1 = RecurringTransaction.builder()
                    .id(recurringId1)
                    .updatedById(userId)
                    .description("Netflix")
                    .amount(15.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(15)
                    .nextOccurrenceDate(LocalDate.now().minusDays(1))
                    .endDate(LocalDate.now().plusMonths(6))
                    .status(RecurringTransactionStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            RecurringTransaction recurring2 = RecurringTransaction.builder()
                    .id(recurringId2)
                    .updatedById(userId)
                    .description("Spotify")
                    .amount(9.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(10)
                    .nextOccurrenceDate(LocalDate.now().minusDays(2))
                    .endDate(LocalDate.now().plusMonths(12))
                    .status(RecurringTransactionStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(recurringTransactionRepository.streamDueRecurringTransactions(any()))
                    .thenReturn(Stream.of(recurring1, recurring2));
            when(transactionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.processDueRecurringTransactions();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
            verify(transactionRepository).saveAll(captor.capture());
            List<Transaction> saved = captor.getValue();

            assertEquals(2, saved.size(), "should create 2 transactions");

            Transaction first = saved.get(0);
            assertEquals(recurringId1, first.recurringTransactionId(), "first transaction should reference recurring txn 1");
            assertEquals("Netflix", first.description(), "first description should match recurring");
            assertEquals(15.99, first.amount(), "first amount should match recurring");
            assertEquals(category, first.category(), "first category should match recurring");
            assertEquals(account, first.account(), "first account should match recurring");
            assertEquals(category.type(), first.type(), "first type should derive from category");

            Transaction second = saved.get(1);
            assertEquals(recurringId2, second.recurringTransactionId(), "second transaction should reference recurring txn 2");
            assertEquals("Spotify", second.description(), "second description should match recurring");
            assertEquals(9.99, second.amount(), "second amount should match recurring");

            verify(transactionRepository, times(1)).flush();
        }

        @Test
        void givenNoDueRecurringTransactions_thenDoesNotCreateAnyTransaction() {
            when(recurringTransactionRepository.streamDueRecurringTransactions(any()))
                    .thenReturn(Stream.empty());

            service.processDueRecurringTransactions();

            verify(transactionRepository, never()).saveAll(any());
        }

        @Test
        void givenBatchExceedsLimit_thenFlushesInBatches() {
            RecurringTransaction recurring = RecurringTransaction.builder()
                    .id(UUID.randomUUID())
                    .updatedById(account.user().id())
                    .description("Subscription")
                    .amount(10.0)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(1)
                    .nextOccurrenceDate(LocalDate.now().minusDays(1))
                    .endDate(LocalDate.now().plusMonths(6))
                    .status(RecurringTransactionStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            List<RecurringTransaction> dueRecurrences = java.util.stream.IntStream.range(0, 75)
                    .mapToObj(i -> recurring.toBuilder().id(UUID.randomUUID()).description("Sub " + i).build())
                    .toList();

            when(recurringTransactionRepository.streamDueRecurringTransactions(any()))
                    .thenReturn(dueRecurrences.stream());
            when(transactionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.processDueRecurringTransactions();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
            verify(transactionRepository, times(2)).saveAll(captor.capture());
            List<List<Transaction>> batches = captor.getAllValues();

            assertEquals(50, batches.get(0).size(), "first batch should be full (50)");
            assertEquals(25, batches.get(1).size(), "second batch should contain remaining 25");

            verify(transactionRepository, times(2)).flush();
        }
    }

    @Nested
    class GetRecurringTransactionsByUserId {

        @Test
        void givenRecurringTransactionsExist_thenReturnsSummariesWithRemainingDaysAndUser() {
            UUID userId = UUID.randomUUID();
            LocalDate nextOccurrenceDate = LocalDate.now().plusDays(10);

            RecurringTransactionSummary summary = RecurringTransactionSummary.builder()
                    .id(UUID.randomUUID())
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(15)
                    .nextOccurrenceDate(nextOccurrenceDate)
                    .endDate(LocalDate.now().plusMonths(6))
                    .transactionStatus(TransactionStatus.UPCOMING)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            User user = User.builder().id(userId).firstName("John").lastName("Doe").build();

            when(recurringTransactionRepository.findSummariesByUserId(eq(userId), any()))
                    .thenReturn(List.of(summary));
            when(userClient.getUsersByIds(List.of(userId))).thenReturn(List.of(user));

            List<RecurringTransactionSummary> result = service.getRecurringTransactionsByUserId(userId);

            assertNotNull(result, "result should not be null");
            assertEquals(1, result.size(), "should return 1 summary");

            RecurringTransactionSummary resultSummary = result.getFirst();
            assertEquals("Monthly subscription", resultSummary.description(), "description should match");
            assertEquals(15.99, resultSummary.amount(), "amount should match");
            assertEquals(category, resultSummary.category(), "category should match");
            assertEquals(account, resultSummary.account(), "account should match");
            assertEquals(TransactionStatus.UPCOMING, resultSummary.transactionStatus(), "transactionStatus should match");
            assertEquals(RecurringTransactionStatus.ACTIVE, resultSummary.status(), "status should match");
            assertEquals(nextOccurrenceDate, resultSummary.nextOccurrenceDate(), "nextOccurrenceDate should match");

            int expectedRemainingDays = (int) ChronoUnit.DAYS.between(LocalDate.now(), nextOccurrenceDate);
            assertEquals(expectedRemainingDays, resultSummary.remainingDays(), "remainingDays should be computed from nextOccurrenceDate");
            assertNotNull(resultSummary.updatedBy(), "user should not be null");
            assertEquals("John", resultSummary.updatedBy().firstName(), "user firstName should match");
            assertEquals("Doe", resultSummary.updatedBy().lastName(), "user lastName should match");

            verify(recurringTransactionRepository).findSummariesByUserId(eq(userId), any());
            verify(userClient).getUsersByIds(List.of(userId));
        }

        @Test
        void givenNextOccurrenceDateIsTomorrow_thenRemainingDaysIsOne() {
            UUID userId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            RecurringTransactionSummary summary = RecurringTransactionSummary.builder()
                    .id(UUID.randomUUID())
                    .description("Monthly subscription")
                    .amount(15.99)
                    .variableAmount(false)
                    .category(category)
                    .account(account)
                    .dayOfMonth(15)
                    .nextOccurrenceDate(tomorrow)
                    .endDate(today.plusMonths(6))
                    .transactionStatus(TransactionStatus.UPCOMING)
                    .status(RecurringTransactionStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            User user = User.builder().id(userId).firstName("John").lastName("Doe").build();

            when(recurringTransactionRepository.findSummariesByUserId(eq(userId), any()))
                    .thenReturn(List.of(summary));
            when(userClient.getUsersByIds(List.of(userId))).thenReturn(List.of(user));

            List<RecurringTransactionSummary> result = service.getRecurringTransactionsByUserId(userId);

            assertNotNull(result, "result should not be null");
            assertEquals(1, result.size(), "should return 1 summary");

            RecurringTransactionSummary resultSummary = result.getFirst();
            assertEquals(1, resultSummary.remainingDays(), "remainingDays should be 1 when nextOccurrenceDate is tomorrow");

            verify(recurringTransactionRepository).findSummariesByUserId(eq(userId), any());
            verify(userClient).getUsersByIds(List.of(userId));
        }

        @Test
        void givenNoRecurringTransactions_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            when(recurringTransactionRepository.findSummariesByUserId(eq(userId), any()))
                    .thenReturn(List.of());
            when(userClient.getUsersByIds(List.of(userId))).thenReturn(List.of());

            List<RecurringTransactionSummary> result = service.getRecurringTransactionsByUserId(userId);

            assertNotNull(result, "result should not be null");
            assertTrue(result.isEmpty(), "should return empty list");

            verify(recurringTransactionRepository).findSummariesByUserId(eq(userId), any());
            verify(userClient).getUsersByIds(List.of(userId));
        }
    }
}
