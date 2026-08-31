package com.fabiankevin.app.persistence;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.CompositeTransactionEventPublisher;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaPartyRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.DefaultTransactionService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.*;

@Import(DefaultTransactionRepositoryTest.TestContextConfiguration.class)
@DataJpaTest
@ActiveProfiles("test")
class DefaultTransactionRepositoryTest {
    @MockitoSpyBean
    private JpaTransactionRepository jpaTransactionRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private JpaCategoryRepository jpaCategoryRepository;
    @Autowired
    private JpaAccountRepository jpaAccountRepository;
    @Autowired
    private TransactionService transactionService;
    private final UUID userId = UUID.randomUUID();

    @TestConfiguration
    public static class TestContextConfiguration {
        @Bean
        public TransactionRepository transactionRepository(JpaTransactionRepository jpaTransactionRepository, EntityManager entityManager) {
            return new DefaultTransactionRepository(jpaTransactionRepository, entityManager);
        }

        @Bean
        public AccountRepository accountRepository(JpaAccountRepository jpaAccountRepository) {
            return new DefaultAccountRepository(jpaAccountRepository);
        }

        @Bean
        public CategoryRepository categoryRepository(JpaCategoryRepository jpaCategoryRepository) {
            return new DefaultCategoryRepository(jpaCategoryRepository);
        }

        @Bean
        public PartyRepository sharedSpaceRepository(JpaPartyRepository jpaPartyRepository) {
            return new DefaultPartyRepository(jpaPartyRepository);
        }

        @Bean
        public UserClient userClient() {
            return mock(UserClient.class);
        }

        @Bean
        public TransactionService transactionService(
                AccountRepository accountRepository,
                CategoryRepository categoryRepository,
                TransactionRepository transactionRepository,
                PartyRepository partyRepository,
                UserClient userClient) {
            return new DefaultTransactionService(
                    accountRepository,
                    categoryRepository,
                    transactionRepository,
                    List.of(),
                    partyRepository,
                    new CompositeTransactionEventPublisher(List.of()),
                    100,
                    userClient);
        }
    }

    @BeforeEach
    void cleanUp() {
        jpaTransactionRepository.deleteAll();
        jpaCategoryRepository.deleteAll();
        jpaAccountRepository.deleteAll();

        clearInvocations(jpaTransactionRepository);
    }
    
    @Nested
    class GetSummaryByYearAndUserIdGroupedByCategory {
        @Test
        void givenTwoCategoriesWithSameYear_shouldReturnTwoCategoriesSummaryPoints() {
            int year = 2026;
            CategoryEntity food = createCategory("FOOD");
            CategoryEntity rent = createCategory("RENT");
            AccountEntity cash = createAccount("CASH");

            List.of(AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(rent.getId())
                            .accountId(cash.getId())
                            .amount(8000)
                            .transactionDate(LocalDate.of(2026, 3, 1))
                            .description("Rent payment")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(50)
                            .transactionDate(LocalDate.of(2026, 5, 15))
                            .description("Food purchase")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(200)
                            .transactionDate(LocalDate.of(2026, 7, 15))
                            .description("Yogurt")
                            .build()).forEach(command -> transactionService.addTransaction(command));

            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = LocalDate.of(year, 12, 31);

            List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, Set.of(userId), TransactionType.EXPENSE);
            Assertions.assertThat(result).hasSize(2);
            Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("FOOD", "RENT");
            Assertions.assertThat(result).extracting(SummaryPoint::total)
                    .as("totals should match")
                    .containsExactlyInAnyOrder(250.0, 8000.0);

            verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, Set.of(userId), TransactionType.EXPENSE);
        }

        @Test
        void givenEmptyStreamable_shouldReturnEmptyList() {
            int year = 2025;
            UUID otherUserId = UUID.randomUUID();

            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = LocalDate.of(year, 12, 31);

            when(jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, Set.of(otherUserId), TransactionType.EXPENSE))
                    .thenReturn(Streamable.empty());

            List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, Set.of(otherUserId), TransactionType.EXPENSE);

            Assertions.assertThat(result).isEmpty();

            verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, Set.of(otherUserId), TransactionType.EXPENSE);
        }
    }

    @Test
    void getSummaryByYearAndMonthGroupedByMonth_givenTwoMonthsWithSameYear_shouldReturnTwoMonthsSummaryPoints() {
        int year = 2026;
        CategoryEntity food = createCategory("FOOD");
        CategoryEntity gadget = createCategory("GADGET");
        AccountEntity cash = createAccount("CASH");

        List.of(AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(250)
                        .transactionDate(LocalDate.of(2026, 3, 1))
                        .description("Some expense")
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(8000)
                        .transactionDate(LocalDate.of(2026, 5, 15))
                        .description("Another expense")
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(gadget.getId())
                        .accountId(cash.getId())
                        .amount(70000)
                        .transactionDate(LocalDate.of(2025, 5, 15))
                        .description("Iphone 15 Pro Max")
                        .build()).forEach(command -> transactionService.addTransaction(command));

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, Set.of(userId), TransactionType.EXPENSE);

        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("3", "5");
        Assertions.assertThat(result).extracting(SummaryPoint::total)
                .containsExactlyInAnyOrder(250.0, 8000.0);

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, Set.of(userId), TransactionType.EXPENSE);
    }

    @Test
    void getSummaryByDateRangeAndUserIdGroupedByYear_givenTwoYearsWithSameUserId_shouldReturnTwoYearsSummaryPoints() {
        CategoryEntity food = createCategory("FOOD");
        AccountEntity cash = createAccount("CASH");

        List.of(AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(10000)
                        .transactionDate(LocalDate.of(2025, 1, 1))
                        .description("New Year Celebration")
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(15000)
                        .transactionDate(LocalDate.of(2026, 1, 1))
                        .description("New Year Celebration")
                        .build()).forEach(command -> transactionService.addTransaction(command));

        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByYear(from, to, Set.of(userId), TransactionType.EXPENSE);

        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("2025", "2026");
        Assertions.assertThat(result).extracting(SummaryPoint::total)
                .as("totals should match")
                .containsExactlyInAnyOrder(10000.0, 15000.0);

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByYear(from, to, Set.of(userId), TransactionType.EXPENSE);
    }

    @Test
    void getSummaryByDateRangeAndUserIdGroupedByYear_givenEmptyStreamable_shouldReturnEmptyList() {
        int year = 2025;
        UUID otherUserId = UUID.randomUUID();

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        when(jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByYear(from, to, Set.of(otherUserId), TransactionType.EXPENSE))
                .thenReturn(Streamable.empty());

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByYear(from, to, Set.of(otherUserId), TransactionType.EXPENSE);

        Assertions.assertThat(result).isEmpty();

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByYear(from, to, Set.of(otherUserId), TransactionType.EXPENSE);
    }

    @Test
    void getSummaryByDateRangeAndUserIdGroupedByDay_givenTwoDaysWithSameMonth_shouldReturnTwoDaysSummaryPoints() {
        int year = 2026;
        CategoryEntity food = createCategory("FOOD");
        AccountEntity cash = createAccount("CASH");

        List.of(AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(250)
                        .transactionDate(LocalDate.of(2026, 3, 1))
                        .description("Some expense")
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(8000)
                        .transactionDate(LocalDate.of(2026, 3, 15))
                        .description("Another expense")
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(70000)
                        .transactionDate(LocalDate.of(2026, 5, 15))
                        .description("Iphone 15 Pro Max")
                        .build()).forEach(command -> transactionService.addTransaction(command));

        LocalDate from = LocalDate.of(year, 3, 1);
        LocalDate to = LocalDate.of(year, 3, 31);

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(userId), TransactionType.EXPENSE);

        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("1", "15");
        Assertions.assertThat(result).extracting(SummaryPoint::total)
                .containsExactlyInAnyOrder(250.0, 8000.0);

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(userId), TransactionType.EXPENSE);
    }

    @Nested
    class GetSummaryByDateRangeAndUserIdGroupedByDayWithTypeNull {

        @Test
        void givenEmptyStreamable_shouldReturnEmptyList() {
            int year = 2025;
            UUID otherUserId = UUID.randomUUID();

            LocalDate from = LocalDate.of(year, 3, 1);
            LocalDate to = LocalDate.of(year, 3, 31);

            when(jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(otherUserId), TransactionType.EXPENSE))
                    .thenReturn(Streamable.empty());

            List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(otherUserId), TransactionType.EXPENSE);

            Assertions.assertThat(result).isEmpty();

            verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(otherUserId), TransactionType.EXPENSE);
        }

        @Test
        void givenIncomeAndExpenseOnSameDay_shouldReturnNetBalance() {
            CategoryEntity salary = createCategory("SALARY_DAY_NULL_1", TransactionType.INCOME);
            CategoryEntity food = createCategory("FOOD_DAY_NULL_1", TransactionType.EXPENSE);
            AccountEntity cash = createAccount("CASH_DAY_NULL_1");

            List.of(
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(salary.getId())
                            .accountId(cash.getId())
                            .amount(5000)
                            .transactionDate(LocalDate.of(2026, 3, 1))
                            .description("Salary")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(500)
                            .transactionDate(LocalDate.of(2026, 3, 1))
                            .description("Food")
                            .build()
            ).forEach(command -> transactionService.addTransaction(command));

            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);

            List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(userId), null);

            Assertions.assertThat(result).hasSize(1);
            Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactly("1");
            Assertions.assertThat(result).extracting(SummaryPoint::total).containsExactly(4500.0);

            verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(userId), null);
        }

        @Test
        void givenMultipleDaysWithMixedTransactions_shouldReturnDailyBalances() {
            CategoryEntity salary = createCategory("SALARY_DAY_NULL_2", TransactionType.INCOME);
            CategoryEntity food = createCategory("FOOD_DAY_NULL_2", TransactionType.EXPENSE);
            CategoryEntity rent = createCategory("RENT_DAY_NULL_2", TransactionType.EXPENSE);
            AccountEntity cash = createAccount("CASH_DAY_NULL_2");

            List.of(
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(salary.getId())
                            .accountId(cash.getId())
                            .amount(5000)
                            .transactionDate(LocalDate.of(2026, 3, 1))
                            .description("Salary")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(500)
                            .transactionDate(LocalDate.of(2026, 3, 1))
                            .description("Food")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(rent.getId())
                            .accountId(cash.getId())
                            .amount(2000)
                            .transactionDate(LocalDate.of(2026, 3, 2))
                            .description("Rent")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(salary.getId())
                            .accountId(cash.getId())
                            .amount(3000)
                            .transactionDate(LocalDate.of(2026, 3, 2))
                            .description("Freelance")
                            .build()
            ).forEach(command -> transactionService.addTransaction(command));

            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);

            List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(userId), null);

            Assertions.assertThat(result).hasSize(2);
            Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("1", "2");
            Assertions.assertThat(result).extracting(SummaryPoint::total).containsExactlyInAnyOrder(4500.0, 1000.0);

            verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(userId), null);
        }

        @Test
        void givenOnlyExpenses_shouldReturnNegativeBalances() {
            CategoryEntity food = createCategory("FOOD_DAY_NULL_3", TransactionType.EXPENSE);
            CategoryEntity rent = createCategory("RENT_DAY_NULL_3", TransactionType.EXPENSE);
            AccountEntity cash = createAccount("CASH_DAY_NULL_3");

            List.of(
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(250)
                            .transactionDate(LocalDate.of(2026, 3, 1))
                            .description("Food")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(rent.getId())
                            .accountId(cash.getId())
                            .amount(8000)
                            .transactionDate(LocalDate.of(2026, 3, 15))
                            .description("Rent")
                            .build()
            ).forEach(command -> transactionService.addTransaction(command));

            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);

            List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(userId), null);

            Assertions.assertThat(result).hasSize(2);
            Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("1", "15");
            Assertions.assertThat(result).extracting(SummaryPoint::total).containsExactlyInAnyOrder(-250.0, -8000.0);

            verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByDay(from, to, Set.of(userId), null);
        }
    }

    @Nested
    class DeleteByIdAndUserId {
        @Test
        void givenExistingTransactionAndMatchingUserId_shouldDeleteAndReturnOne() {
            CategoryEntity food = createCategory("FOOD");
            AccountEntity cash = createAccount("CASH");

            AddTransactionCommand command = AddTransactionCommand.builder()
                    .userId(userId)
                    .categoryId(food.getId())
                    .accountId(cash.getId())
                    .amount(500)
                    .transactionDate(LocalDate.of(2026, 5, 1))
                    .description("Test transaction")
                    .build();
            Transaction saved = transactionService.addTransaction(command);

            int deleted = transactionRepository.deleteByIdAndUserId(saved.id(), userId);

            Assertions.assertThat(deleted).isEqualTo(1);
            Assertions.assertThat(transactionRepository.findById(saved.id())).isEmpty();
            verify(jpaTransactionRepository, times(1)).deleteByIdAndAccountUserId(saved.id(), userId);
        }

        @Test
        void givenExistingTransactionWithDifferentUserId_shouldNotDeleteAndReturnZero() {
            CategoryEntity food = createCategory("FOOD");
            AccountEntity cash = createAccount("CASH");

            AddTransactionCommand command = AddTransactionCommand.builder()
                    .userId(userId)
                    .categoryId(food.getId())
                    .accountId(cash.getId())
                    .amount(500)
                    .transactionDate(LocalDate.of(2026, 5, 1))
                    .description("Test transaction")
                    .build();
            Transaction saved = transactionService.addTransaction(command);

            UUID differentUserId = UUID.randomUUID();

            int deleted = transactionRepository.deleteByIdAndUserId(saved.id(), differentUserId);

            Assertions.assertThat(deleted).isEqualTo(0);
            Assertions.assertThat(transactionRepository.findById(saved.id())).isPresent();

            verify(jpaTransactionRepository, times(1)).deleteByIdAndAccountUserId(saved.id(), differentUserId);
        }
    }

    @Nested
    class GetTransactionsByPageAndUserIdAndType {
        @Test
        void givenExpenseTypeFilter_thenShouldReturnOnlyExpenses() {
            CategoryEntity food = createCategory("FOOD", TransactionType.EXPENSE);
            CategoryEntity salary = createCategory("SALARY", TransactionType.INCOME);
            AccountEntity cash = createAccount("CASH");
            UUID recurringTransactionId = UUID.randomUUID();

            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(LocalDate.of(2026, 1, 1)).description("expense1").recurringTransactionId(recurringTransactionId).build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(salary.getId()).accountId(cash.getId()).amount(5000).transactionDate(LocalDate.of(2026, 1, 2)).description("income1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(200).transactionDate(LocalDate.of(2026, 1, 3)).description("expense2").build()
            ).forEach(command -> transactionService.addTransaction(command));

            PageQuery query = new PageQuery(0, 10, "transactionDate", "ASC");

            Page<Transaction> page = transactionRepository.getTransactionsByPageAndUserIdAndType(query, Set.of(userId), TransactionType.EXPENSE);

            Assertions.assertThat(page).isNotNull();
            Assertions.assertThat(page.content()).hasSize(2);
            Assertions.assertThat(page.totalElements()).isEqualTo(2);
            Assertions.assertThat(page.content()).allMatch(t -> t.type() == TransactionType.EXPENSE);
            Assertions.assertThat(page.content()).extracting(Transaction::description).containsExactly("expense1", "expense2");
            Assertions.assertThat(page.content().get(0).recurringTransactionId()).isEqualTo(recurringTransactionId);
            Assertions.assertThat(page.content().get(1).recurringTransactionId()).isNull();

            verify(jpaTransactionRepository, times(1)).findAllByUserIdsAndType(eq(Set.of(userId)), eq(TransactionType.EXPENSE), any(Pageable.class));
        }

        @Test
        void givenIncomeTypeFilter_thenShouldReturnOnlyIncomes() {
            CategoryEntity food = createCategory("FOOD", TransactionType.EXPENSE);
            CategoryEntity salary = createCategory("SALARY", TransactionType.INCOME);
            AccountEntity cash = createAccount("CASH");

            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(LocalDate.of(2026, 1, 1)).description("expense1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(salary.getId()).accountId(cash.getId()).amount(5000).transactionDate(LocalDate.of(2026, 1, 2)).description("income1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(salary.getId()).accountId(cash.getId()).amount(3000).transactionDate(LocalDate.of(2026, 1, 3)).description("income2").build()
            ).forEach(command -> transactionService.addTransaction(command));

            com.fabiankevin.app.services.queries.PageQuery query = new com.fabiankevin.app.services.queries.PageQuery(0, 10, "transactionDate", "ASC");

            com.fabiankevin.app.models.Page<Transaction> page = transactionRepository.getTransactionsByPageAndUserIdAndType(query, Set.of(userId), TransactionType.INCOME);

            Assertions.assertThat(page).isNotNull();
            Assertions.assertThat(page.content()).hasSize(2);
            Assertions.assertThat(page.totalElements()).isEqualTo(2);
            Assertions.assertThat(page.content()).allMatch(t -> t.type() == TransactionType.INCOME);
            Assertions.assertThat(page.content()).extracting(Transaction::description).containsExactly("income1", "income2");

            verify(jpaTransactionRepository, times(1)).findAllByUserIdsAndType(eq(Set.of(userId)), eq(TransactionType.INCOME), any(Pageable.class));
        }

        @Test
        void givenNullType_thenShouldReturnAllTransactions() {
            CategoryEntity food = createCategory("FOOD");
            CategoryEntity salary = createCategory("SALARY");
            AccountEntity cash = createAccount("CASH");

            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(LocalDate.of(2026, 1, 1)).description("expense1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(salary.getId()).accountId(cash.getId()).amount(5000).transactionDate(LocalDate.of(2026, 1, 2)).description("income1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(200).transactionDate(LocalDate.of(2026, 1, 3)).description("expense2").build()
            ).forEach(command -> transactionService.addTransaction(command));

            com.fabiankevin.app.services.queries.PageQuery query = new com.fabiankevin.app.services.queries.PageQuery(0, 10, "transactionDate", "ASC");

            com.fabiankevin.app.models.Page<Transaction> page = transactionRepository.getTransactionsByPageAndUserIdAndType(query, Set.of(userId), null);

            Assertions.assertThat(page).isNotNull();
            Assertions.assertThat(page.content()).hasSize(3);
            Assertions.assertThat(page.totalElements()).isEqualTo(3);
            Assertions.assertThat(page.content()).extracting(Transaction::description).containsExactly("expense1", "income1", "expense2");

            verify(jpaTransactionRepository, times(1)).findAllByUserIdsAndType(eq(Set.of(userId)), eq(null), any(Pageable.class));
        }

        @Test
        void givenNonMatchingType_thenShouldReturnEmptyPage() {
            CategoryEntity food = createCategory("FOOD");
            AccountEntity cash = createAccount("CASH");

            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(LocalDate.of(2026, 1, 1)).description("expense1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(200).transactionDate(LocalDate.of(2026, 1, 2)).description("expense2").build()
            ).forEach(command -> transactionService.addTransaction(command));

            PageQuery query = new PageQuery(0, 10, "transactionDate", "ASC");

            Page<Transaction> page = transactionRepository.getTransactionsByPageAndUserIdAndType(query, Set.of(userId), TransactionType.INCOME);

            Assertions.assertThat(page).isNotNull();
            Assertions.assertThat(page.content()).isEmpty();
            Assertions.assertThat(page.totalElements()).isEqualTo(0);
            Assertions.assertThat(page.totalPages()).isEqualTo(0);

            verify(jpaTransactionRepository, times(1)).findAllByUserIdsAndType(eq(Set.of(userId)), eq(TransactionType.INCOME), any(Pageable.class));
        }

        @Test
        void givenWrongUserId_thenShouldReturnEmptyPage() {
            CategoryEntity food = createCategory("FOOD");
            AccountEntity cash = createAccount("CASH");

            AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(LocalDate.of(2026, 1, 1)).description("expense1").build();
            transactionService.addTransaction(AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(LocalDate.of(2026, 1, 1)).description("expense1").build());

            UUID differentUserId = UUID.randomUUID();
            PageQuery query = new PageQuery(0, 10, "transactionDate", "ASC");

            com.fabiankevin.app.models.Page<Transaction> page = transactionRepository.getTransactionsByPageAndUserIdAndType(query, Set.of(differentUserId), TransactionType.EXPENSE);

            Assertions.assertThat(page).isNotNull();
            Assertions.assertThat(page.content()).isEmpty();
            Assertions.assertThat(page.totalElements()).isEqualTo(0);

            verify(jpaTransactionRepository, times(1)).findAllByUserIdsAndType(eq(Set.of(differentUserId)), eq(TransactionType.EXPENSE), any(Pageable.class));
        }
    }

    @Nested
    class GetTransactionsByPageAndUserId {
        @Test
        void givenMultipleTransactions_thenShouldReturnPagedResults() {
            CategoryEntity food = createCategory("FOOD");
            AccountEntity cash = createAccount("CASH");

            // create 3 transactions
            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(LocalDate.of(2026, 1, 1)).description("t1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(200).transactionDate(LocalDate.of(2026, 1, 2)).description("t2").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(300).transactionDate(LocalDate.of(2026, 1, 3)).description("t3").build()
            ).forEach(command -> transactionService.addTransaction(command));

            com.fabiankevin.app.services.queries.PageQuery query = new com.fabiankevin.app.services.queries.PageQuery(0, 2, "transactionDate", "ASC");

            com.fabiankevin.app.models.Page<Transaction> page = transactionRepository.getTransactionsByPageAndUserId(query, Set.of(userId));

            Assertions.assertThat(page).isNotNull();
            Assertions.assertThat(page.content()).hasSize(2);
            Assertions.assertThat(page.page()).isEqualTo(0);
            Assertions.assertThat(page.size()).isEqualTo(2);
            Assertions.assertThat(page.totalElements()).isEqualTo(3);
            Assertions.assertThat(page.totalPages()).isEqualTo(2);
        }
    }

    @Nested
    class SumByTypeAndUserId {
        @Test
        void givenExpensesWithAllFilters_shouldReturnCorrectSum() {
            CategoryEntity food = createCategory("FOOD");
            AccountEntity cash = createAccount("CASH");

            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(LocalDate.of(2026, 3, 1)).description("t1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(200).transactionDate(LocalDate.of(2026, 3, 15)).description("t2").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(300).transactionDate(LocalDate.of(2026, 3, 25)).description("t3").build()
            ).forEach(command -> transactionService.addTransaction(command));

            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);

            var result = transactionRepository.sumByTypeAndUserId(Set.of(userId), from, to, cash.getId(), food.getId());

            double expenseTotal = result.stream()
                    .filter(p -> p.label().equals("EXPENSE"))
                    .mapToDouble(SummaryPoint::total)
                    .sum();
            double incomeTotal = result.stream()
                    .filter(p -> p.label().equals("INCOME"))
                    .mapToDouble(SummaryPoint::total)
                    .sum();

            Assertions.assertThat(expenseTotal).isEqualTo(600.0);
            Assertions.assertThat(incomeTotal).isEqualTo(0.0);

            verify(jpaTransactionRepository, times(1)).sumByTypeAndDateRange(eq(Set.of(userId)), eq(from), eq(to), eq(cash.getId()), eq(food.getId()));
        }

        @Test
        void givenExpensesWithNullOptionalFilters_shouldAggregateAcrossCategories() {
            CategoryEntity food = createCategory("FOOD");
            CategoryEntity rent = createCategory("RENT");
            AccountEntity cash = createAccount("CASH");

            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(LocalDate.of(2026, 3, 1)).description("food1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(200).transactionDate(LocalDate.of(2026, 3, 15)).description("food2").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(rent.getId()).accountId(cash.getId()).amount(5000).transactionDate(LocalDate.of(2026, 3, 10)).description("rent").build()
            ).forEach(command -> transactionService.addTransaction(command));

            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);

            var result = transactionRepository.sumByTypeAndUserId(Set.of(userId), from, to, null, null);

            double expenseTotal = result.stream()
                    .filter(p -> p.label().equals("EXPENSE"))
                    .mapToDouble(SummaryPoint::total)
                    .sum();
            double incomeTotal = result.stream()
                    .filter(p -> p.label().equals("INCOME"))
                    .mapToDouble(SummaryPoint::total)
                    .sum();

            Assertions.assertThat(expenseTotal).isEqualTo(5300.0);
            Assertions.assertThat(incomeTotal).isEqualTo(0.0);

            verify(jpaTransactionRepository, times(1)).sumByTypeAndDateRange(eq(Set.of(userId)), eq(from), eq(to), eq(null), eq(null));
        }

        @Test
        void givenNoMatchingTransactions_shouldReturnZero() {
            CategoryEntity food = createCategory("FOOD");
            AccountEntity cash = createAccount("CASH");

            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);

            doReturn(Streamable.empty()).when(jpaTransactionRepository)
                    .sumByTypeAndDateRange(eq(Set.of(userId)), eq(from), eq(to), eq(cash.getId()), eq(food.getId()));

            var result = transactionRepository.sumByTypeAndUserId(Set.of(userId), from, to, cash.getId(), food.getId());

            double incomeTotal = result.stream()
                    .filter(p -> p.label().equals("INCOME"))
                    .mapToDouble(SummaryPoint::total)
                    .sum();
            double expenseTotal = result.stream()
                    .filter(p -> p.label().equals("EXPENSE"))
                    .mapToDouble(SummaryPoint::total)
                    .sum();

            Assertions.assertThat(incomeTotal).isEqualTo(0.0);
            Assertions.assertThat(expenseTotal).isEqualTo(0.0);

            verify(jpaTransactionRepository, times(1)).sumByTypeAndDateRange(eq(Set.of(userId)), eq(from), eq(to), eq(cash.getId()), eq(food.getId()));
        }

        @Test
        void givenIncomeAndExpenses_shouldReturnBothTotals() {
            AccountEntity cash = createAccount("CASH");
            CategoryEntity salary = createCategory("SALARY", TransactionType.INCOME);
            CategoryEntity food = createCategory("FOOD");

            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(salary.getId()).accountId(cash.getId()).amount(5000).transactionDate(LocalDate.of(2026, 3, 1)).description("salary").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(500).transactionDate(LocalDate.of(2026, 3, 5)).description("food1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(300).transactionDate(LocalDate.of(2026, 3, 15)).description("food2").build()
            ).forEach(command -> transactionService.addTransaction(command));

            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);

            var result = transactionRepository.sumByTypeAndUserId(Set.of(userId), from, to, cash.getId(), null);

            double incomeTotal = result.stream()
                    .filter(p -> p.label().equals("INCOME"))
                    .mapToDouble(SummaryPoint::total)
                    .sum();
            double expenseTotal = result.stream()
                    .filter(p -> p.label().equals("EXPENSE"))
                    .mapToDouble(SummaryPoint::total)
                    .sum();

            Assertions.assertThat(incomeTotal).isEqualTo(5000.0);
            Assertions.assertThat(expenseTotal).isEqualTo(800.0);

            verify(jpaTransactionRepository, times(1)).sumByTypeAndDateRange(eq(Set.of(userId)), eq(from), eq(to), eq(cash.getId()), eq(null));
        }

        @Test
        void givenMultipleTransactionsWithSameCategory_shouldReturnAggregatedTotals() {
            CategoryEntity food = createCategory("FOOD");
            CategoryEntity rent = createCategory("RENT");
            AccountEntity cash = createAccount("CASH");

            List.of(AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(100)
                            .transactionDate(LocalDate.of(2026, 6, 1))
                            .description("Food 1")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(150)
                            .transactionDate(LocalDate.of(2026, 6, 15))
                            .description("Food 2")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(rent.getId())
                            .accountId(cash.getId())
                            .amount(8000)
                            .transactionDate(LocalDate.of(2026, 6, 1))
                            .description("Rent")
                            .build()).forEach(command -> transactionService.addTransaction(command));

            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);

            List<SummaryPoint> result = transactionRepository.sumByTypeAndUserId(Set.of(userId), from, to, food.getId());

            Assertions.assertThat(result).hasSize(1);
            Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactly("EXPENSE");
            Assertions.assertThat(result).extracting(SummaryPoint::total).containsExactly(250.0);

            verify(jpaTransactionRepository, times(1)).sumByTypeAndDateRangeByCategory(eq(Set.of(userId)), eq(from), eq(to), eq(food.getId()));
        }

        @Test
        void givenNoMatchingTransactionsForCategory_shouldReturnEmptyList() {
            CategoryEntity food = createCategory("FOOD");
            CategoryEntity other = createCategory("OTHER");
            AccountEntity cash = createAccount("CASH");

            AddTransactionCommand.builder()
                    .userId(userId)
                    .categoryId(food.getId())
                    .accountId(cash.getId())
                    .amount(100)
                    .transactionDate(LocalDate.of(2026, 6, 1))
                    .description("Food")
                    .build();

            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);

            when(jpaTransactionRepository.sumByTypeAndDateRangeByCategory(eq(Set.of(userId)), eq(from), eq(to), eq(other.getId())))
                    .thenReturn(Streamable.empty());

            List<SummaryPoint> result = transactionRepository.sumByTypeAndUserId(Set.of(userId), from, to, other.getId());

            Assertions.assertThat(result).isEmpty();

            verify(jpaTransactionRepository, times(1)).sumByTypeAndDateRangeByCategory(eq(Set.of(userId)), eq(from), eq(to), eq(other.getId()));
        }

        @Test
        void givenTransactionsAcrossMultipleMonths_shouldReturnAggregatedTotals() {
            CategoryEntity food = createCategory("FOOD");
            AccountEntity cash = createAccount("CASH");

            List.of(AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(100)
                            .transactionDate(LocalDate.of(2026, 4, 1))
                            .description("April food")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(200)
                            .transactionDate(LocalDate.of(2026, 5, 15))
                            .description("May food")
                            .build(),
                    AddTransactionCommand.builder()
                            .userId(userId)
                            .categoryId(food.getId())
                            .accountId(cash.getId())
                            .amount(300)
                            .transactionDate(LocalDate.of(2026, 6, 1))
                            .description("June food")
                            .build()).forEach(command -> transactionService.addTransaction(command));

            LocalDate from = LocalDate.of(2026, 4, 1);
            LocalDate to = LocalDate.of(2026, 6, 30);

            List<SummaryPoint> result = transactionRepository.sumByTypeAndUserId(Set.of(userId), from, to, food.getId());

            Assertions.assertThat(result).hasSize(1);
            Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactly("EXPENSE");
            Assertions.assertThat(result).extracting(SummaryPoint::total).containsExactly(600.0);

            verify(jpaTransactionRepository, times(1)).sumByTypeAndDateRangeByCategory(eq(Set.of(userId)), eq(from), eq(to), eq(food.getId()));
        }
    }

    @Nested
    class SumBalance {
        @Test
        void givenIncomeAndExpenses_shouldReturnNetBalance() {
            AccountEntity cash = createAccount("CASH");
            CategoryEntity salary = createCategory("SALARY", TransactionType.INCOME);
            CategoryEntity food = createCategory("FOOD");

            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(salary.getId()).accountId(cash.getId()).amount(5000).transactionDate(LocalDate.of(2026, 3, 1)).description("salary").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(500).transactionDate(LocalDate.of(2026, 3, 5)).description("food1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(300).transactionDate(LocalDate.of(2026, 3, 15)).description("food2").build()
            ).forEach(command -> transactionService.addTransaction(command));

            double result = transactionRepository.sumBalance(Set.of(userId));

            Assertions.assertThat(result).isEqualTo(4200.0);

            verify(jpaTransactionRepository, times(1)).sumBalance(eq(Set.of(userId)));
        }

        @Test
        void givenTransactionsAcrossMultipleAccounts_shouldSumAll() {
            AccountEntity cash = createAccount("CASH");
            AccountEntity savings = createAccount("SAVINGS");
            CategoryEntity salary = createCategory("SALARY", TransactionType.INCOME);
            CategoryEntity food = createCategory("FOOD");

            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(salary.getId()).accountId(cash.getId()).amount(5000).transactionDate(LocalDate.of(2026, 3, 1)).description("salary to cash").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(salary.getId()).accountId(savings.getId()).amount(3000).transactionDate(LocalDate.of(2026, 3, 1)).description("salary to savings").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(500).transactionDate(LocalDate.of(2026, 3, 5)).description("food from cash").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(savings.getId()).amount(200).transactionDate(LocalDate.of(2026, 3, 10)).description("food from savings").build()
            ).forEach(command -> transactionService.addTransaction(command));

            double result = transactionRepository.sumBalance(Set.of(userId));

            Assertions.assertThat(result).isEqualTo(7300.0);

            verify(jpaTransactionRepository, times(1)).sumBalance(eq(Set.of(userId)));
        }

        @Test
        void givenNoMatchingTransactions_shouldReturnZero() {
            doReturn(0.0).when(jpaTransactionRepository).sumBalance(Set.of(userId));

            double result = transactionRepository.sumBalance(Set.of(userId));

            Assertions.assertThat(result).isEqualTo(0.0);

            verify(jpaTransactionRepository, times(1)).sumBalance(eq(Set.of(userId)));
        }
    }

    @Nested
    class GetDailyAveragePastWeek {
        @Test
        void givenTransactionsWithinPastWeek_shouldReturnDailyAverage() {
            CategoryEntity food = createCategory("Food & Drinks");
            AccountEntity cash = createAccount("Cash Wallet");

            LocalDate today = LocalDate.now();
            List.of(
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(100).transactionDate(today.minusDays(1)).description("t1").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(200).transactionDate(today.minusDays(2)).description("t2").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(300).transactionDate(today.minusDays(3)).description("t3").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(400).transactionDate(today.minusDays(4)).description("t4").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(500).transactionDate(today.minusDays(5)).description("t5").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(600).transactionDate(today.minusDays(6)).description("t6").build(),
                    AddTransactionCommand.builder().userId(userId).categoryId(food.getId()).accountId(cash.getId()).amount(700).transactionDate(today.minusDays(7)).description("t7").build()
            ).forEach(command -> transactionService.addTransaction(command));

            LocalDate startDate = today.minusDays(7);

            List<SummaryPoint> result = transactionRepository.getDailyAveragePastWeek(Set.of(userId));

            Assertions.assertThat(result).hasSize(1);
            SummaryPoint summaryPoint = result.getFirst();
            Assertions.assertThat(summaryPoint.label()).isEqualTo(userId.toString());
            Assertions.assertThat(summaryPoint.total()).isCloseTo(1.0, offset(0.01));

            verify(jpaTransactionRepository, times(1)).getDailyAveragePastWeek(Set.of(userId), startDate);
        }

        @Test
        void givenNoTransactionsInRange_shouldReturnEmptyList() {
            UUID otherUserId = UUID.randomUUID();

            List<SummaryPoint> result = transactionRepository.getDailyAveragePastWeek(Set.of(otherUserId));

            Assertions.assertThat(result).isEmpty();

            verify(jpaTransactionRepository, times(1)).getDailyAveragePastWeek(Set.of(otherUserId), LocalDate.now().minusDays(7));
        }
    }

    @Nested
    class FindByRecurringTransactionId {
        @Test
        void givenTransactionWithRecurringId_shouldReturnTransaction() {
            CategoryEntity food = createCategory("FOOD");
            AccountEntity cash = createAccount("CASH");
            UUID recurringId = UUID.randomUUID();

            Transaction saved = transactionService.addTransaction(AddTransactionCommand.builder()
                    .userId(userId)
                    .categoryId(food.getId())
                    .accountId(cash.getId())
                    .amount(500)
                    .transactionDate(LocalDate.of(2026, 5, 1))
                    .description("Recurring payment")
                    .recurringTransactionId(recurringId)
                    .build());

            Optional<Transaction> result = transactionRepository.findByRecurringTransactionId(recurringId);

            Assertions.assertThat(result).isPresent();
            Assertions.assertThat(result.get().id()).isEqualTo(saved.id());
            Assertions.assertThat(result.get().recurringTransactionId()).isEqualTo(recurringId);
            verify(jpaTransactionRepository, times(1)).findByRecurringTransactionId(recurringId);
        }

        @Test
        void givenNoTransactionWithRecurringId_shouldReturnEmpty() {
            UUID recurringId = UUID.randomUUID();

            Optional<Transaction> result = transactionRepository.findByRecurringTransactionId(recurringId);

            Assertions.assertThat(result).isEmpty();
            verify(jpaTransactionRepository, times(1)).findByRecurringTransactionId(recurringId);
        }
    }

    private CategoryEntity createCategory(String categoryName) {
        return createCategory(categoryName, TransactionType.EXPENSE);
    }

    private CategoryEntity createCategory(String categoryName, TransactionType type) {
        CategoryEntity category = new CategoryEntity();
        category.setName(categoryName);
        category.setTransactionType(type);
        category.setUserId(userId);
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());

        return jpaCategoryRepository.saveAndFlush(category);
    }

    private AccountEntity createAccount(String name) {
        AccountEntity account = new AccountEntity();
        account.setUserId(userId);
        account.setName(name);
        account.setCurrency("PHP");
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());

        return jpaAccountRepository.saveAndFlush(account);
    }
}
