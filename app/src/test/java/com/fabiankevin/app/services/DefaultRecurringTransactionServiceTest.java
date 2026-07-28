package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.RecurringTransactionRepository;
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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

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
                .userId(userId)
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
                .categoryId(category.id())
                .accountId(account.id())
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

            RecurringTransactionSummary summary = service.create(command);

            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime expectedEndDate = now.plusMonths(command.durationMonths());

            assertNotNull(summary, "summary should not be null");
            assertNotNull(summary.id(), "id should be generated");
            assertEquals(command.userId(), summary.userId(), "userId should match command");
            assertEquals("Monthly subscription", summary.description(), "description should match command");
            assertEquals(15.99, summary.amount(), "amount should match command");
            assertEquals(15, summary.dayOfMonth(), "dayOfMonth should match command");
            ZonedDateTime expectedNextOccurrenceDate = now.getDayOfMonth() < command.dayOfMonth()
                    ? now.withDayOfMonth(command.dayOfMonth())
                    : now.plusMonths(1).withDayOfMonth(command.dayOfMonth());

            assertEquals(TransactionStatus.UPCOMING, summary.transactionStatus(), "status should be UPCOMING when nextOccurrenceDate is in the future");
            assertEquals(RecurringTransactionStatus.ACTIVE, summary.status(), "recurring status should be ACTIVE");
            assertNotNull(summary.category(), "category should not be null");
            assertEquals(category.id(), summary.category().id(), "category should match");
            assertNotNull(summary.account(), "account should not be null");
            assertEquals(account.id(), summary.account().id(), "account should match");
            assertNotNull(summary.createdAt(), "createdAt should not be null");
            assertNotNull(summary.updatedAt(), "updatedAt should not be null");
            assertEquals(expectedEndDate.truncatedTo(ChronoUnit.HOURS), summary.endDate().truncatedTo(ChronoUnit.HOURS), "endDate should be now plus durationMonths");
            assertEquals(expectedNextOccurrenceDate.truncatedTo(ChronoUnit.HOURS), summary.nextOccurrenceDate().truncatedTo(ChronoUnit.HOURS), "nextOccurrenceDate should be derived from dayOfMonth");

            ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringTransactionRepository).save(captor.capture());
            RecurringTransaction saved = captor.getValue();
            assertEquals(command.userId(), saved.userId(), "saved userId should match command");
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
    }
}
