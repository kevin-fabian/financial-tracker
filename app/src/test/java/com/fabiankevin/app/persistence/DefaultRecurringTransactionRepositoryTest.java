package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionSummary;
import com.fabiankevin.app.models.recurring_transactions.TransactionStatus;
import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaRecurringTransactionRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import(DefaultRecurringTransactionRepository.class)
class DefaultRecurringTransactionRepositoryTest {

    @MockitoSpyBean
    private JpaRecurringTransactionRepository jpaRecurringTransactionRepository;

    @Autowired
    private JpaAccountRepository jpaAccountRepository;

    @Autowired
    private JpaCategoryRepository jpaCategoryRepository;

    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    private RecurringTransaction recurringTransaction;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        AccountEntity account = jpaAccountRepository.saveAndFlush(AccountEntity.builder()
                .name("Checking")
                .userId(userId)
                .currency("USD")
                .type(AccountType.CASH.name())
                .createdAt(now)
                .updatedAt(now)
                .build());
        CategoryEntity category = jpaCategoryRepository.saveAndFlush(CategoryEntity.builder()
                .name("GROCERIES")
                .transactionType(TransactionType.EXPENSE)
                .userId(userId)
                .icon("local_grocery_store")
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());

        recurringTransaction = RecurringTransaction.builder()
                .updatedById(userId)
                .description("Monthly subscription")
                .amount(15.99)
                .category(category.toModel())
                .account(account.toModel())
                .dayOfMonth(15)
                .nextOccurrenceDate(LocalDate.of(2026, 8, 15))
                .endDate(null)
                .status(RecurringTransactionStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Mockito.reset(jpaRecurringTransactionRepository);
    }

    @Nested
    class Save {
        @Test
        void givenValidRecurringTransaction_persistsAndRetrievesAllFields() {
            RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);

            assertNotNull(saved.id(), "recurring transaction id should have been generated");
            assertEquals(recurringTransaction.description(), saved.description(), "description should match");
            assertEquals(recurringTransaction.amount(), saved.amount(), "amount should match");
            assertEquals(recurringTransaction.dayOfMonth(), saved.dayOfMonth(), "dayOfMonth should match");
            assertEquals(recurringTransaction.status(), saved.status(), "status should match");
            assertEquals(recurringTransaction.category().id(), saved.category().id(), "category should match");
            assertEquals(recurringTransaction.account().id(), saved.account().id(), "account should match");
            assertEquals(recurringTransaction.nextOccurrenceDate(), saved.nextOccurrenceDate(), "nextOccurrenceDate should match");
            assertEquals(recurringTransaction.endDate(), saved.endDate(), "endDate should match");
            assertNotNull(saved.createdAt(), "createdAt should not be null");
            assertNotNull(saved.updatedAt(), "updatedAt should not be null");

            verify(jpaRecurringTransactionRepository, times(1)).save(any());
        }

        @Test
        void givenNull_shouldThrowInvalidDataAccessApiUsageException() {
            Assertions.assertThatThrownBy(() -> recurringTransactionRepository.save(null))
                    .as("saving null should throw InvalidDataAccessApiUsageException")
                    .isInstanceOf(InvalidDataAccessApiUsageException.class);

            verify(jpaRecurringTransactionRepository, times(1)).save(any());
        }
    }

    @Nested
    class FindSummaries {

        @Test
        void givenUserIdWithPaidRecurringTransaction_thenReturnsSummaryWithPaidStatus() {
            RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
            UUID userId = saved.updatedById();
            Instant now = Instant.now();

            Account account = saved.account();
            Category category = saved.category();

            jpaTransactionRepository.saveAndFlush(TransactionEntity.builder()
                    .account(AccountEntity.builder().id(account.id()).name(account.name()).userId(account.user().id())
                            .currency(account.currency().getCurrencyCode()).type(account.type().name())
                            .active(account.active()).createdAt(now).updatedAt(now).build())
                    .category(CategoryEntity.builder().id(category.id()).name(category.name())
                            .transactionType(category.type()).userId(category.userId()).icon(category.icon())
                            .active(category.active()).createdAt(now).updatedAt(now).build())
                    .amount(15.99)
                    .description("Monthly subscription")
                    .transactionDate(LocalDate.of(2026, 1, 15))
                    .recurringTransactionId(saved.id())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());

            LocalDate referenceNow = LocalDate.of(2026, 9, 1);

            List<RecurringTransactionSummary> summaries = recurringTransactionRepository.findSummariesByUserId(userId, referenceNow);

            Assertions.assertThat(summaries).hasSize(1);
            RecurringTransactionSummary summary = summaries.getFirst();
            assertEquals(0, summary.remainingDays(), "remainingDays initial value should be zero");
            assertEquals(saved.id(), summary.id(), "summary id should match saved recurring transaction id");
            assertEquals("Monthly subscription", summary.description(), "description should match");
            assertEquals(15.99, summary.amount(), "amount should match");
            assertEquals(15, summary.dayOfMonth(), "dayOfMonth should match");
            assertEquals(TransactionStatus.PAID, summary.transactionStatus(), "status should be PAID when transaction exists and nextOccurrenceDate is in the past");
            assertEquals(RecurringTransactionStatus.ACTIVE, summary.status(), "recurring transaction status should match");
            assertEquals(saved.nextOccurrenceDate(), summary.nextOccurrenceDate(), "nextOccurrenceDate should match");
            assertEquals(saved.endDate(), summary.endDate(), "endDate should match");
            assertNotNull(summary.category(), "category should not be null");
            assertEquals(category.id(), summary.category().id(), "category id should match");
            assertNotNull(summary.account(), "account should not be null");
            assertEquals(account.id(), summary.account().id(), "account id should match");

            verify(jpaRecurringTransactionRepository, times(1)).findAllSummariesByUserId(userId, referenceNow);
        }

        @Test
        void givenOverdueRecurringTransactionWithNoLinkedTransaction_thenReturnsSummaryWithOverdueStatus() {
            RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
            UUID userId = saved.updatedById();

            LocalDate referenceNow = LocalDate.of(2026, 9, 1);

            List<RecurringTransactionSummary> summaries = recurringTransactionRepository.findSummariesByUserId(userId, referenceNow);

            Assertions.assertThat(summaries).hasSize(1);
            assertEquals(TransactionStatus.OVERDUE, summaries.getFirst().transactionStatus(),
                    "status should be OVERDUE when no linked transaction exists and nextOccurrenceDate is in the past");

            verify(jpaRecurringTransactionRepository, times(1)).findAllSummariesByUserId(userId, referenceNow);
        }

        @Test
        void givenUpcomingRecurringTransaction_thenReturnsSummaryWithUpcomingStatus() {
            RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
            UUID userId = saved.updatedById();

            LocalDate referenceNow = LocalDate.of(2026, 8, 1);

            List<RecurringTransactionSummary> summaries = recurringTransactionRepository.findSummariesByUserId(userId, referenceNow);

            Assertions.assertThat(summaries).hasSize(1);
            assertEquals(TransactionStatus.UPCOMING, summaries.getFirst().transactionStatus(),
                    "status should be UPCOMING when nextOccurrenceDate is in the future");

            verify(jpaRecurringTransactionRepository, times(1)).findAllSummariesByUserId(userId, referenceNow);
        }

        @Test
        void givenRecurringTransactionOwnedByOtherUser_thenReturnsEmptyList() {
            recurringTransactionRepository.save(recurringTransaction);
            UUID otherUserId = UUID.randomUUID();

            List<RecurringTransactionSummary> summaries = recurringTransactionRepository.findSummariesByUserId(otherUserId, LocalDate.now());

            Assertions.assertThat(summaries).isEmpty();

            verify(jpaRecurringTransactionRepository, times(1)).findAllSummariesByUserId(eq(otherUserId), any());
        }

        @Test
        void givenUserIdWithNoRecurringTransactions_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            List<RecurringTransactionSummary> summaries = recurringTransactionRepository.findSummariesByUserId(userId, LocalDate.now());

            Assertions.assertThat(summaries).isEmpty();

            verify(jpaRecurringTransactionRepository, times(1)).findAllSummariesByUserId(eq(userId), any());
        }
    }
}
