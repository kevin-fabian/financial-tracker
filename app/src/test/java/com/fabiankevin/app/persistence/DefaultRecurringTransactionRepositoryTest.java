package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.models.recurring_transactions.RecurringTransactionStatus;
import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaRecurringTransactionRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
                .description("Monthly subscription")
                .amount(15.99)
                .transactionType(TransactionType.EXPENSE)
                .category(category.toModel())
                .account(account.toModel())
                .dayOfMonth(15)
                .nextOccurrenceDate(ZonedDateTime.of(2026, 8, 15, 0, 0, 0, 0, ZoneId.of("UTC")))
                .startDate(ZonedDateTime.of(2026, 1, 15, 0, 0, 0, 0, ZoneId.of("UTC")))
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
            assertEquals(recurringTransaction.transactionType(), saved.transactionType(), "transactionType should match");
            assertEquals(recurringTransaction.dayOfMonth(), saved.dayOfMonth(), "dayOfMonth should match");
            assertEquals(recurringTransaction.status(), saved.status(), "status should match");
            assertEquals(recurringTransaction.category().id(), saved.category().id(), "category should match");
            assertEquals(recurringTransaction.account().id(), saved.account().id(), "account should match");
            assertEquals(recurringTransaction.nextOccurrenceDate(), saved.nextOccurrenceDate(), "nextOccurrenceDate should match");
            assertEquals(recurringTransaction.startDate(), saved.startDate(), "startDate should match");
            assertEquals(recurringTransaction.endDate(), saved.endDate(), "endDate should match");
            assertNotNull(saved.createdAt(), "createdAt should not be null");
            assertNotNull(saved.updatedAt(), "updatedAt should not be null");

            verify(jpaRecurringTransactionRepository, times(1)).save(any());
        }

        @Test
        void givenNull_shouldThrowInvalidDataAccessApiUsageException() {
            Assertions.assertThatThrownBy(() -> recurringTransactionRepository.save(null))
                    .as("saving null should throw InvalidDataAccessApiUsageException")
                    .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class);

            verify(jpaRecurringTransactionRepository, times(1)).save(any());
        }
    }
}
