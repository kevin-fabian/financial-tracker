package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.queries.PageQuery;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.fabiankevin.app.models.enums.AccountType.BANK_ACCOUNT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(DefaultAccountRepositoryTest.ContextConfiguration.class)
@DataJpaTest
class DefaultAccountRepositoryTest {

    @MockitoSpyBean
    private JpaAccountRepository jpaAccountRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account account;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public AccountRepository accountRepository(JpaAccountRepository jpaAccountRepository) {
            return new DefaultAccountRepository(jpaAccountRepository);
        }
    }

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .name("GCASH")
                .user(User.of(UUID.randomUUID()))
                .currency(Currency.getInstance("PHP"))
                .type(AccountType.E_WALLET)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void save_givenValidAccount_shouldPersistAndRetrieve() {
        Account saved = accountRepository.save(account);

        var found = accountRepository.findById(saved.id()).orElseThrow();

        Assertions.assertThat(found)
                .as("found account should match saved account ignoring id")
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(saved);

        verify(jpaAccountRepository, times(1)).save(any());
        verify(jpaAccountRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenExistingAccount_shouldReturnAccount() {
        Account saved = accountRepository.save(account);

        var found = accountRepository.findById(saved.id()).orElseThrow();

        Assertions.assertThat(found)
                .as("found account should match saved account ignoring id")
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(saved);

        verify(jpaAccountRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExisting_shouldReturnEmptyOptional() {
        var found = accountRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }

    @Test
    void deleteById_givenExistingAccount_shouldRemoveAccount() {
        Account saved = accountRepository.save(account);

        accountRepository.deleteById(saved.id());

        Optional<Account> found = accountRepository.findById(saved.id());
        Assertions.assertThat(found).as("account should be deleted and retrieval should return empty optional").isEmpty();

        verify(jpaAccountRepository, times(1)).deleteById(saved.id());
    }

    @Test
    void getAccountsByPageAndUserId_givenMultipleAccounts_thenShouldReturnPagedResults() {
        UUID userId = UUID.randomUUID();

        // create and save 5 accounts for the same user
        for (int i = 0; i < 5; i++) {
            Account a = Account.builder()
                    .name("Account " + i)
                    .user(User.of(userId))
                    .currency(java.util.Currency.getInstance("PHP"))
                    .type(com.fabiankevin.app.models.enums.AccountType.E_WALLET)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            accountRepository.save(a);
        }

        PageQuery query = new PageQuery(0, 3, "name", "ASC");
        Page<Account> page = accountRepository.getAccountsByPageAndUserId(query, userId);

        Assertions.assertThat(page.content()).as("page should contain 3 elements").hasSize(3);
        Assertions.assertThat(page.totalElements()).isEqualTo(5);
        Assertions.assertThat(page.page()).isZero();
        Assertions.assertThat(page.size()).isEqualTo(3);

        verify(jpaAccountRepository, times(1)).findAllByUserId(userId, PageRequest.of(0, 3, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.fromString("ASC"), "name")));
    }

    @Nested
    class FindAllByPageQueryWithSummaryTest {
        @Autowired
        private JpaTransactionRepository jpaTransactionRepository;

        @Autowired
        private JpaCategoryRepository jpaCategoryRepository;

        @Test
        void findAllByPageQueryWithSummary_givenMultipleNewAccounts_shouldReturnPagedSummaries() {
            UUID userId = UUID.randomUUID();

            // create accounts
            Account acc1 = Account.builder()
                    .name("Account 1")
                    .user(User.of(userId))
                    .currency(java.util.Currency.getInstance("PHP"))
                    .type(BANK_ACCOUNT)
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            Account acc2 = Account.builder()
                    .name("Account 2")
                    .user(User.of(userId))
                    .currency(java.util.Currency.getInstance("PHP"))
                    .type(BANK_ACCOUNT)
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            Account acc3 = Account.builder()
                    .name("Account 3")
                    .user(User.of(userId))
                    .currency(java.util.Currency.getInstance("PHP"))
                    .type(BANK_ACCOUNT)
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            acc1 = accountRepository.save(acc1);
            acc2 = accountRepository.save(acc2);
            acc3 = accountRepository.save(acc3);

            // create categories: EXPENSE for acc1, INCOME for acc2, EXPENSE for acc3
            java.time.LocalDate today = LocalDate.now();
            CategoryEntity expenseCategory1 = jpaCategoryRepository.save(CategoryEntity.builder()
                    .name("Expense 1").transactionType(TransactionType.EXPENSE).userId(userId)
                    .createdAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC))
                    .updatedAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)).build());
            CategoryEntity incomeCategory = jpaCategoryRepository.save(CategoryEntity.builder()
                    .name("Income 1").transactionType(TransactionType.INCOME).userId(userId)
                    .createdAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC))
                    .updatedAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)).build());
            CategoryEntity expenseCategory3 = jpaCategoryRepository.save(CategoryEntity.builder()
                    .name("Expense 3").transactionType(TransactionType.EXPENSE).userId(userId)
                    .createdAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC))
                    .updatedAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)).build());

            // transactions with categories:
            // acc1: 2x EXPENSE 50 => totalBalance = -100
            // acc2: 1x INCOME 200 => totalBalance = +200
            // acc3: 1x EXPENSE 300 => totalBalance = -300
            // net total = -200 (absolute sum for percentage = 600)
            jpaTransactionRepository.save(TransactionEntity.builder()
                    .amount(50)
                    .transactionDate(today)
                    .account(jpaAccountRepository.findById(acc1.id()).orElseThrow())
                    .category(expenseCategory1).build());
            jpaTransactionRepository.save(TransactionEntity.builder()
                    .amount(50)
                    .transactionDate(today)
                    .account(jpaAccountRepository.findById(acc1.id()).orElseThrow())
                    .category(expenseCategory1).build());
            jpaTransactionRepository.save(TransactionEntity.builder()
                    .amount(50)
                    .transactionDate(today)
                    .account(jpaAccountRepository.findById(acc2.id()).orElseThrow())
                    .category(incomeCategory).build());
            jpaTransactionRepository.save(TransactionEntity.builder()
                    .amount(300)
                    .transactionDate(today)
                    .account(jpaAccountRepository.findById(acc3.id()).orElseThrow())
                    .category(expenseCategory3).build());

            PageQuery query = new PageQuery(0, 10, "name", "ASC");
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
            Page<AccountSummary> page = accountRepository.findAllByPageQueryWithSummary(query, List.of(userId), monthStart, monthEnd);

            Assertions.assertThat(page.content()).as("page should contain 3 summaries").hasSize(3);
            Assertions.assertThat(page.totalElements()).isEqualTo(3);
            Assertions.assertThat(page.page()).isZero();
            Assertions.assertThat(page.size()).isEqualTo(10);

            Map<String, AccountSummary> byName = page.content().stream()
                    .collect(Collectors.toMap(accountSummary -> accountSummary.account().name(), s -> s));

            AccountSummary s1 = byName.get("Account 1");
            AccountSummary s2 = byName.get("Account 2");
            AccountSummary s3 = byName.get("Account 3");

            Assertions.assertThat(s1.totalBalance()).as("Account 1 totalBalance (-100)").isEqualTo(-100.0);
            Assertions.assertThat(s1.totalTransactions()).as("Account 1 totalTransactions").isEqualTo(2);

            Assertions.assertThat(s2.totalBalance()).as("Account 2 totalBalance (+200)").isEqualTo(50.0);
            Assertions.assertThat(s2.totalTransactions()).as("Account 2 totalTransactions").isEqualTo(1);

            Assertions.assertThat(s3.totalBalance()).as("Account 3 totalBalance (-300)").isEqualTo(-300.0);
            Assertions.assertThat(s3.totalTransactions()).as("Account 3 totalTransactions").isEqualTo(1);
        }

        @Test
        void findAllByPageQueryWithSummary_givenSingleAccountWithTransactions_shouldReturnCorrectTotals() {
            UUID userId = UUID.randomUUID();

            Account account = Account.builder()
                    .name("Solo Account")
                    .user(User.of(userId))
                    .currency(java.util.Currency.getInstance("USD"))
                    .type(AccountType.CASH)
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            account = accountRepository.save(account);

            // create EXPENSE category
            java.time.LocalDate today = java.time.LocalDate.now();
            CategoryEntity expenseCategory = jpaCategoryRepository.save(CategoryEntity.builder()
                    .name("Expense").transactionType(TransactionType.EXPENSE).userId(userId)
                    .createdAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC))
                    .updatedAt(today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)).build());

            // 2 EXPENSE transactions: 100 + 50 => totalBalance = -150
            jpaTransactionRepository.save(TransactionEntity.builder()
                    .amount(100)
                    .transactionDate(today)
                    .account(jpaAccountRepository.findById(account.id()).orElseThrow())
                    .category(expenseCategory).build());
            jpaTransactionRepository.save(TransactionEntity.builder()
                    .amount(50)
                    .transactionDate(today)
                    .account(jpaAccountRepository.findById(account.id()).orElseThrow())
                    .category(expenseCategory).build());

            PageQuery query = new PageQuery(0, 10, "name", "ASC");
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
            Page<AccountSummary> page = accountRepository.findAllByPageQueryWithSummary(query, List.of(userId), monthStart, monthEnd);

            Assertions.assertThat(page.content()).as("page should contain 1 summary").hasSize(1);
            AccountSummary summary = page.content().getFirst();
            Assertions.assertThat(summary.totalBalance()).as("totalBalance (-150)").isEqualTo(-150.0);
            Assertions.assertThat(summary.totalTransactions()).as("totalTransactions").isEqualTo(2);
        }

        @Test
        void findAllByPageQueryWithSummary_givenNoAccounts_shouldReturnEmptyPage() {
            UUID userId = UUID.randomUUID();

            PageQuery query = new PageQuery(0, 10, "name", "ASC");
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
            Page<AccountSummary> page = accountRepository.findAllByPageQueryWithSummary(query, List.of(userId), monthStart, monthEnd);

            Assertions.assertThat(page.content()).as("page should be empty when no accounts exist").isEmpty();
            Assertions.assertThat(page.totalElements()).isZero();
            Assertions.assertThat(page.totalPages()).isZero();
        }
    }

    @Nested
    class DeleteAllByUserId {
        @Test
        void givenExistingAccountsForUser_shouldDeleteAllAndReturnCount() {
            UUID userId = UUID.randomUUID();

            for (int i = 0; i < 3; i++) {
                Account a = Account.builder()
                        .name("Account " + i)
                        .user(User.of(userId))
                        .currency(java.util.Currency.getInstance("PHP"))
                        .type(com.fabiankevin.app.models.enums.AccountType.E_WALLET)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                accountRepository.save(a);
            }

            long deleted = accountRepository.deleteAllByUserId(userId);

            Assertions.assertThat(deleted).as("should return the number of deleted accounts").isEqualTo(3);
            Assertions.assertThat(accountRepository.findAllByNamesIn(List.of("Account 0", "Account 1", "Account 2")))
                    .as("all accounts for user should be deleted")
                    .isEmpty();

            verify(jpaAccountRepository, times(1)).deleteAllByUserId(userId);
        }

        @Test
        void givenNonExistingUserId_shouldDeleteNothingAndReturnZero() {
            UUID nonExistingUserId = UUID.randomUUID();

            long deleted = accountRepository.deleteAllByUserId(nonExistingUserId);

            Assertions.assertThat(deleted).as("should return 0 when no accounts exist for user").isZero();

            verify(jpaAccountRepository, times(1)).deleteAllByUserId(nonExistingUserId);
        }
    }
}
