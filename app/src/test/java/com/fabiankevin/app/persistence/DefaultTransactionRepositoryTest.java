package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Amount;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.DefaultTransactionService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.util.Streamable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    public static class ContextConfiguration {
        @Bean
        public TransactionRepository transactionRepository(JpaTransactionRepository jpaTransactionRepository) {
            return new DefaultTransactionRepository(jpaTransactionRepository);
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
        public TransactionService transactionService(
                AccountRepository accountRepository,
                CategoryRepository categoryRepository,
                TransactionRepository transactionRepository) {
            return new DefaultTransactionService(
                    accountRepository,
                    categoryRepository,
                    transactionRepository,
                    List.of());
        }
    }

    @Test
    void getSummaryByYearAndUserIdGroupedByCategory_givenTwoCategoriesWithSameYear_shouldReturnTwoCategoriesSummaryPoints() {
        int year = 2026;
        CategoryEntity food = createCategory("FOOD");
        CategoryEntity rent = createCategory("RENT");
        AccountEntity cash = createAccount("CASH");

        List.of(AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(rent.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(8000, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2026, 3, 1))
                        .description("Rent payment")
                        .type(TransactionType.EXPENSE)
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(50, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2026, 5, 15))
                        .description("Food purchase")
                        .type(TransactionType.EXPENSE)
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(200, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2026, 7, 15))
                        .description("Yogurt")
                        .type(TransactionType.EXPENSE)
                        .build()).forEach(transactionService::addTransaction);

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, List.of(userId));
        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("FOOD", "RENT");
        Assertions.assertThat(result).extracting(SummaryPoint::total)
                .as("totals should match ignoring scale")
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(BigDecimal.valueOf(250), BigDecimal.valueOf(8000));

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, List.of(userId));
    }

    @Test
    void getSummaryByYearAndUserIdGroupedByCategory_givenEmptyStreamable_shouldReturnEmptyList() {
        int year = 2025;
        UUID otherUserId = UUID.randomUUID();

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        when(jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, List.of(otherUserId)))
                .thenReturn(Streamable.empty());

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, List.of(otherUserId));

        Assertions.assertThat(result).isEmpty();

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, List.of(otherUserId));
    }

    @Test
    void getSummaryByYearAndMonthGroupedByMonth_givenTwoMonthsWithSameYear_shouldReturnTwoMonthsSummaryPoints() {
        int year = 2026;
        CategoryEntity food = createCategory("FOOD");
        CategoryEntity gadget = createCategory("GADGET");;
        AccountEntity cash = createAccount("CASH");

        List.of(AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(250, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2026, 3, 1))
                        .description("Some expense")
                        .type(TransactionType.EXPENSE)
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(8000, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2026, 5, 15))
                        .description("Another expense")
                        .type(TransactionType.EXPENSE)
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(gadget.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(70000, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2025, 5, 15))
                        .description("Iphone 15 Pro Max")
                        .type(TransactionType.EXPENSE)
                        .build()).forEach(transactionService::addTransaction);

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId));

        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("3", "5");
        Assertions.assertThat(result).extracting(SummaryPoint::total)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(BigDecimal.valueOf(250), BigDecimal.valueOf(8000));

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId));
    }

    @Test
    void getSummaryByDateRangeAndUserIdGroupedByYear_givenTwoYearsWithSameUserId_shouldReturnTwoYearsSummaryPoints() {
        CategoryEntity food = createCategory("FOOD");
        AccountEntity cash = createAccount("CASH");

        List.of(AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(10000, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2025, 1, 1))
                        .description("New Year Celebration")
                        .type(TransactionType.EXPENSE)
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(15000, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2026, 1, 1))
                        .description("New Year Celebration")
                        .type(TransactionType.EXPENSE)
                        .build()).forEach(transactionService::addTransaction);

        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByYear(from, to, List.of(userId));

        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("2025", "2026");
        Assertions.assertThat(result).extracting(SummaryPoint::total)
                .as("totals should match ignoring scale")
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(BigDecimal.valueOf(10000), BigDecimal.valueOf(15000));

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByYear(from, to, List.of(userId));
    }

    @Test
    void getSummaryByDateRangeAndUserIdGroupedByYear_givenEmptyStreamable_shouldReturnEmptyList() {
        int year = 2025;
        UUID otherUserId = UUID.randomUUID();

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        when(jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByYear(from, to, List.of(otherUserId)))
                .thenReturn(Streamable.empty());

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByYear(from, to, List.of(otherUserId));

        Assertions.assertThat(result).isEmpty();

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByYear(from, to, List.of(otherUserId));
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
                        .amount(Amount.of(250, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2026, 3, 1))
                        .description("Some expense")
                        .type(TransactionType.EXPENSE)
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(8000, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2026, 3, 15))
                        .description("Another expense")
                        .type(TransactionType.EXPENSE)
                        .build(),
                AddTransactionCommand.builder()
                        .userId(userId)
                        .categoryId(food.getId())
                        .accountId(cash.getId())
                        .amount(Amount.of(70000, Currency.getInstance("PHP")))
                        .transactionDate(LocalDate.of(2026, 5, 15))
                        .description("Iphone 15 Pro Max")
                        .type(TransactionType.EXPENSE)
                        .build()).forEach(transactionService::addTransaction);

        LocalDate from = LocalDate.of(year, 3, 1);
        LocalDate to = LocalDate.of(year, 3, 31);

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, List.of(userId));

        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("1", "15");
        Assertions.assertThat(result).extracting(SummaryPoint::total)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(BigDecimal.valueOf(250), BigDecimal.valueOf(8000));

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByDay(from, to, List.of(userId));
    }

    @Test
    void getSummaryByDateRangeAndUserIdGroupedByDay_givenEmptyStreamable_shouldReturnEmptyList() {
        int year = 2025;
        UUID otherUserId = UUID.randomUUID();

        LocalDate from = LocalDate.of(year, 3, 1);
        LocalDate to = LocalDate.of(year, 3, 31);

        when(jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, List.of(otherUserId)))
                .thenReturn(Streamable.empty());

        List<SummaryPoint> result = transactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, List.of(otherUserId));

        Assertions.assertThat(result).isEmpty();

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByDay(from, to, List.of(otherUserId));
    }

    private CategoryEntity createCategory(String categoryName) {
        CategoryEntity category = new CategoryEntity();
        category.setName(categoryName);
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
