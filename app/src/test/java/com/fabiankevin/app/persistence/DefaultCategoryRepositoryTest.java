package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.AccountEntity;
import com.fabiankevin.app.persistence.entities.CategoryEntity;
import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.entities.embeddables.AmountEmbeddable;
import com.fabiankevin.app.persistence.jpa_repositories.JpaAccountRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaCategoryRepository;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.persistence.projections.CategorySummaryProjection;
import com.fabiankevin.app.services.queries.PageQuery;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DataJpaTest
@Import(DefaultCategoryRepository.class)
class DefaultCategoryRepositoryTest {
    @MockitoSpyBean
    private JpaCategoryRepository jpaCategoryRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private JpaTransactionRepository jpaTransactionRepository;
    @Autowired
    private JpaAccountRepository jpaAccountRepository;
    private Category category;
    private Category foodCategory;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        foodCategory = Category.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(UUID.randomUUID())
                .icon("attach_money")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        categoryRepository.save(foodCategory);

        Mockito.reset(jpaCategoryRepository);
    }

    @Test
    void save_givenValidCategory_shouldPersistAndRetrieve() {
        Category saved = categoryRepository.save(category);

        var found = categoryRepository.findByIdAndUserId(saved.id(), saved.userId()).orElseThrow();

        assertNotNull(found.id(), "category id should have been generated");
        assertEquals(category.name(), found.name(), "category name should match");
        assertEquals(category.type(), found.type(), "category type should match");
        assertNotNull(category.createdAt(), "createdAt should have been generated");
        assertNotNull(category.updatedAt(), "updatedAt should have been generated");
        assertNull(category.icon(), "icon should be null when not provided");
        verify(jpaCategoryRepository, times(1)).save(any());
        verify(jpaCategoryRepository, times(1)).findByIdAndUserId(saved.id(), saved.userId());
    }

    @Test
    void save_givenCategoryWithIcon_shouldPersistIconAsColumn() {
        Category categoryWithIcon = Category.builder()
                .name("RESTAURANTS")
                .type(TransactionType.EXPENSE)
                .userId(category.userId())
                .icon("restaurant")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category saved = categoryRepository.save(categoryWithIcon);
        jpaCategoryRepository.flush();

        var found = categoryRepository.findByIdAndUserId(saved.id(), saved.userId()).orElseThrow();

        assertNotNull(found.icon(), "icon should be persisted as string column");
        assertEquals("restaurant", found.icon());
    }

    @Test
    void save_givenNull_shouldThrowIllegalArgumentException() {
        Assertions.assertThatThrownBy(() -> categoryRepository.save(null))
                .as("saving null should throw InvalidDataAccessApiUsageException")
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @Test
    void save_givenExistingCategoryWithDifferentUser_thenSaveCategory() {
        categoryRepository.save(category);

        Category sameCategoryWithDifferentUser = Category.builder()
                .name("FOOD")
                .type(TransactionType.EXPENSE)
                .userId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertDoesNotThrow(() -> categoryRepository.save(sameCategoryWithDifferentUser),
                "should save");

        Assertions.assertThat(jpaCategoryRepository.findAll())
                .hasSize(3)
                .as("category names should be `FOOD`")
                .extracting("name")
                .containsExactlyInAnyOrder("FOOD", "FOOD", "FOOD");
    }

    @Test
    void deleteById_givenExistingCategory_shouldRemoveCategory() {
        Category saved = categoryRepository.save(category);

        categoryRepository.deleteByIdAndUserId(saved.id(), saved.userId());

        Optional<Category> found = categoryRepository.findByIdAndUserId(saved.id(), saved.userId());
        Assertions.assertThat(found).as("category should be deleted and retrieval should return empty optional").isEmpty();

        verify(jpaCategoryRepository, times(1)).deleteByIdAndUserId(saved.id(), saved.userId());
    }

    @Test
    void findById_givenNonExisting_shouldReturnEmptyOptional() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var found = categoryRepository.findByIdAndUserId(id, userId);

        Assertions.assertThat(found).as("non existing id returns empty optional").isEmpty();
    }


    @Test
    void findById_givenExisting_shouldReturnCategory() {
        Category saved = categoryRepository.save(category);
        Category found = categoryRepository.findById(saved.id()).get();

        Assertions.assertThat(found).as("non existing id returns empty optional")
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(saved);
    }

    @Test
    void findAllByPageQuery_givenMultipleCategories_shouldReturnPagedResults() {
        UUID userId = UUID.randomUUID();
        // create 5 categories for the same user
        List<String> names = List.of("A", "B", "C", "D", "E");
        for (String n : names) {
            categoryRepository.save(Category.builder()
                    .name(n)
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }
        // another category for different user (should be ignored)
        categoryRepository.save(Category.builder()
                .name("Z")
                .type(TransactionType.EXPENSE)
                .userId(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        var page0 = categoryRepository.findAllByPageQuery(new PageQuery(0, 2, "name", "ASC"), userId, TransactionType.EXPENSE);

        Assertions.assertThat(page0.content()).hasSize(2);
        Assertions.assertThat(page0.content().stream().map(Category::name).toList())
                .containsExactly("A", "B");
        Assertions.assertThat(page0.page()).isEqualTo(0);
        Assertions.assertThat(page0.size()).isEqualTo(2);
        Assertions.assertThat(page0.totalElements()).isEqualTo(5);
        Assertions.assertThat(page0.totalPages()).isEqualTo(3);
        Assertions.assertThat(page0.first()).isTrue();
        Assertions.assertThat(page0.last()).isFalse();

        verify(jpaCategoryRepository, times(1)).findAllByUserIdAndTransactionType(eq(userId), eq(TransactionType.EXPENSE), any());
    }

    @Test
    void findAllByPageQuery_givenLastPage_shouldReturnRemainingElements() {
        UUID userId = UUID.randomUUID();
        List<String> names = List.of("A", "B", "C", "D", "E");
        for (String n : names) {
            categoryRepository.save(Category.builder()
                    .name(n)
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        var page2 = categoryRepository.findAllByPageQuery(new PageQuery(2, 2, "name", "ASC"), userId, TransactionType.EXPENSE);

        Assertions.assertThat(page2.content()).hasSize(1);
        Assertions.assertThat(page2.content().stream().map(Category::name).toList())
                .containsExactly("E");
        Assertions.assertThat(page2.page()).isEqualTo(2);
        Assertions.assertThat(page2.first()).isFalse();
        Assertions.assertThat(page2.last()).isTrue();

        verify(jpaCategoryRepository, times(1)).findAllByUserIdAndTransactionType(eq(userId), eq(TransactionType.EXPENSE), any());
    }

    @Test
    void findAllByPageQuery_givenDescSort_shouldReturnDescendingOrder() {
        UUID userId = UUID.randomUUID();
        List<String> names = List.of("A", "B", "C", "D", "E");
        for (String n : names) {
            categoryRepository.save(Category.builder()
                    .name(n)
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        var page = categoryRepository.findAllByPageQuery(new PageQuery(0, 3, "name", "DESC"), userId, TransactionType.EXPENSE);

        Assertions.assertThat(page.content()).hasSize(3);
        Assertions.assertThat(page.content().stream().map(Category::name).toList())
                .containsExactly("E", "D", "C");

        verify(jpaCategoryRepository, times(1)).findAllByUserIdAndTransactionType(eq(userId), eq(TransactionType.EXPENSE), any());
    }

    @Test
    void findAllByPageQuery_givenInvalidDirection_shouldThrow() {
        UUID userId = UUID.randomUUID();
        categoryRepository.save(Category.builder()
                .name("A1")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        Assertions.assertThatThrownBy(() -> categoryRepository.findAllByPageQuery(new PageQuery(0, 2, "name", "INVALID"), userId, TransactionType.EXPENSE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findAllByPageQuery_givenTypeFilter_shouldReturnFilteredCategories() {
        UUID userId = UUID.randomUUID();
        // create EXPENSE categories
        categoryRepository.save(Category.builder().name("FOOD").type(TransactionType.EXPENSE).userId(userId).createdAt(Instant.now()).updatedAt(Instant.now()).build());
        categoryRepository.save(Category.builder().name("RENT").type(TransactionType.EXPENSE).userId(userId).createdAt(Instant.now()).updatedAt(Instant.now()).build());
        // create INCOME category
        categoryRepository.save(Category.builder().name("SALARY").type(TransactionType.INCOME).userId(userId).createdAt(Instant.now()).updatedAt(Instant.now()).build());

        var page = categoryRepository.findAllByPageQuery(new PageQuery(0, 10, "name", "ASC"), userId, TransactionType.EXPENSE);

        Assertions.assertThat(page.content()).hasSize(2);
        Assertions.assertThat(page.content().stream().map(Category::name).toList())
                .containsExactly("FOOD", "RENT");

        verify(jpaCategoryRepository, times(1)).findAllByUserIdAndTransactionType(eq(userId), eq(TransactionType.EXPENSE), any());
    }

    @Test
    void findAllByPageQuery_givenTypeFilterNoMatch_shouldReturnEmptyPage() {
        UUID userId = UUID.randomUUID();
        categoryRepository.save(Category.builder().name("FOOD").type(TransactionType.EXPENSE).userId(userId).createdAt(Instant.now()).updatedAt(Instant.now()).build());

        var page = categoryRepository.findAllByPageQuery(new PageQuery(0, 10, "name", "ASC"), userId, TransactionType.INCOME);

        Assertions.assertThat(page.content()).isEmpty();

        verify(jpaCategoryRepository, times(1)).findAllByUserIdAndTransactionType(eq(userId), eq(TransactionType.INCOME), any());
    }

    @Test
    void findAllByPageQuery_givenNullType_shouldReturnAllCategories() {
        UUID userId = UUID.randomUUID();
        categoryRepository.save(Category.builder().name("FOOD").type(TransactionType.EXPENSE).userId(userId).createdAt(Instant.now()).updatedAt(Instant.now()).build());
        categoryRepository.save(Category.builder().name("SALARY").type(TransactionType.INCOME).userId(userId).createdAt(Instant.now()).updatedAt(Instant.now()).build());

        var page = categoryRepository.findAllByPageQuery(new PageQuery(0, 10, "name", "ASC"), userId, null);

        Assertions.assertThat(page.content())
                .hasSize(2);

        verify(jpaCategoryRepository, times(1)).findAllByUserId(eq(userId), any());
    }

    @Test
    void findInactiveByNameAndTypeAndUserId_givenInactiveMatchingCategory_shouldReturnCategory() {
        UUID userId = category.userId();
        String name = category.name();
        TransactionType type = category.type();

        Category inactiveCategory = category.toBuilder()
                .id(null)
                .active(false)
                .build();
        Category saved = categoryRepository.save(inactiveCategory);

        var found = categoryRepository.findInactiveByNameAndTypeAndUserId(name, type, userId);

        Assertions.assertThat(found).as("should return the inactive category").isPresent();
        Assertions.assertThat(found.get().id()).isEqualTo(saved.id());
        Assertions.assertThat(found.get().active()).isFalse();

        verify(jpaCategoryRepository, times(1)).findFirstByActiveFalseAndNameAndTransactionTypeAndUserId(name, type, userId);
    }

    @Test
    void findInactiveByNameAndTypeAndUserId_givenActiveCategory_shouldReturnEmpty() {
        UUID userId = category.userId();
        String name = category.name();
        TransactionType type = category.type();

        Category activeCategory = category.toBuilder()
                .id(null)
                .active(true)
                .build();
        categoryRepository.save(activeCategory);

        var found = categoryRepository.findInactiveByNameAndTypeAndUserId(name, type, userId);

        Assertions.assertThat(found).as("should return empty when category is active").isEmpty();

        verify(jpaCategoryRepository, times(1)).findFirstByActiveFalseAndNameAndTransactionTypeAndUserId(name, type, userId);
    }

    @Test
    void findInactiveByNameAndTypeAndUserId_givenNonExistingCategory_shouldReturnEmpty() {
        UUID userId = UUID.randomUUID();

        var found = categoryRepository.findInactiveByNameAndTypeAndUserId("NONEXISTENT", TransactionType.EXPENSE, userId);

        Assertions.assertThat(found).as("should return empty when no matching category exists").isEmpty();

        verify(jpaCategoryRepository, times(1)).findFirstByActiveFalseAndNameAndTransactionTypeAndUserId("NONEXISTENT", TransactionType.EXPENSE, userId);
    }

    @Test
    void existsByNameAndTypeAndUserId_givenMatchingCategory_shouldReturnTrue() {
        UUID userId = category.userId();
        String name = category.name();
        TransactionType type = category.type();

        categoryRepository.save(category);

        Assertions.assertThat(categoryRepository.existsByNameAndTypeAndUserId(name, type, userId))
                .as("should return true for matching name, type, and userId")
                .isTrue();

        verify(jpaCategoryRepository, times(1)).existsByNameAndTransactionTypeAndUserId(name, type, userId);
    }

    @Test
    void existsByNameAndTypeAndUserId_givenDifferentUserId_shouldReturnFalse() {
        String name = category.name();
        TransactionType type = category.type();
        UUID differentUserId = UUID.randomUUID();

        categoryRepository.save(category);

        Assertions.assertThat(categoryRepository.existsByNameAndTypeAndUserId(name, type, differentUserId))
                .as("should return false when userId does not match")
                .isFalse();

        verify(jpaCategoryRepository, times(1)).existsByNameAndTransactionTypeAndUserId(name, type, differentUserId);
    }

    @Test
    void existsByNameAndTypeAndUserId_givenDifferentType_shouldReturnFalse() {
        UUID userId = category.userId();
        String name = category.name();
        TransactionType differentType = TransactionType.INCOME;

        categoryRepository.save(category);

        Assertions.assertThat(categoryRepository.existsByNameAndTypeAndUserId(name, differentType, userId))
                .as("should return false when type does not match")
                .isFalse();

        verify(jpaCategoryRepository, times(1)).existsByNameAndTransactionTypeAndUserId(name, differentType, userId);
    }

    @Test
    void existsByNameAndTypeAndUserId_givenDifferentName_shouldReturnFalse() {
        UUID userId = category.userId();
        TransactionType type = category.type();
        String differentName = "GROCERIES";

        categoryRepository.save(category);

        Assertions.assertThat(categoryRepository.existsByNameAndTypeAndUserId(differentName, type, userId))
                .as("should return false when name does not match")
                .isFalse();

        verify(jpaCategoryRepository, times(1)).existsByNameAndTransactionTypeAndUserId(differentName, type, userId);
    }

    @Test
    void existsByNameAndTypeAndUserId_givenNonExistingCategory_shouldReturnFalse() {
        UUID userId = UUID.randomUUID();

        Assertions.assertThat(categoryRepository.existsByNameAndTypeAndUserId("NONEXISTENT", TransactionType.EXPENSE, userId))
                .as("should return false when no matching category exists")
                .isFalse();

        verify(jpaCategoryRepository, times(1)).existsByNameAndTransactionTypeAndUserId("NONEXISTENT", TransactionType.EXPENSE, userId);
    }

    @Test
    void findByUserIdWithSummary_givenNoCategories_shouldReturnEmptyList() {
        UUID userId = UUID.randomUUID();
        List<CategorySummaryProjection> result = jpaCategoryRepository.findByUserIdWithSummary(userId);

        Assertions.assertThat(result).as("should return empty list when user has no categories").isEmpty();

        verify(jpaCategoryRepository, times(1)).findByUserIdWithSummary(userId);
    }

    @Test
    void findByUserIdWithSummary_givenCategoriesWithTransactions_shouldReturnCorrectSummary() {
        UUID userId = UUID.randomUUID();

        CategoryEntity food = CategoryEntity.builder()
                .name("FOOD")
                .transactionType(TransactionType.EXPENSE)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        CategoryEntity rent = CategoryEntity.builder()
                .name("RENT")
                .transactionType(TransactionType.EXPENSE)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        CategoryEntity salary = CategoryEntity.builder()
                .name("SALARY")
                .transactionType(TransactionType.INCOME)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        jpaCategoryRepository.saveAll(List.of(food, rent, salary));
        jpaCategoryRepository.flush();

        // Create test transactions directly via JPA to avoid service-layer side effects
        var account = AccountEntity.builder()
                .userId(userId)
                .name("CASH")
                .currency("PHP")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        account = jpaAccountRepository.saveAndFlush(account);

        var foodTx = TransactionEntity.builder()
                .account(account)
                .category(food)
                .amount(new AmountEmbeddable(500.0, "PHP"))
                .transactionDate(java.time.LocalDate.of(2026, 7, 1))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        var rentTx = TransactionEntity.builder()
                .account(account)
                .category(rent)
                .amount(new AmountEmbeddable(2000.0, "PHP"))
                .transactionDate(java.time.LocalDate.of(2026, 7, 1))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        var salaryTx = TransactionEntity.builder()
                .account(account)
                .category(salary)
                .amount(new AmountEmbeddable(5000.0, "PHP"))
                .transactionDate(java.time.LocalDate.of(2026, 7, 1))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        var foodTx2 = TransactionEntity.builder()
                .account(account)
                .category(food)
                .amount(new AmountEmbeddable(300.0, "PHP"))
                .transactionDate(java.time.LocalDate.of(2026, 7, 2))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        var allTransactions = List.of(foodTx, rentTx, salaryTx, foodTx2);
        for (var tx : allTransactions) {
            jpaTransactionRepository.saveAndFlush(tx);
        }

        List<CategorySummaryProjection> result = jpaCategoryRepository.findByUserIdWithSummary(userId);

        Assertions.assertThat(result).hasSize(3);

        var foodSummary = result.stream().filter(p -> "FOOD".equals(p.name())).findFirst().orElseThrow();
        Assertions.assertThat(foodSummary.amount()).isEqualTo(800.0);
        Assertions.assertThat(foodSummary.totalTransactions()).isEqualTo(2);

        var rentSummary = result.stream().filter(p -> "RENT".equals(p.name())).findFirst().orElseThrow();
        Assertions.assertThat(rentSummary.amount()).isEqualTo(2000.0);
        Assertions.assertThat(rentSummary.totalTransactions()).isEqualTo(1);

        var salarySummary = result.stream().filter(p -> "SALARY".equals(p.name())).findFirst().orElseThrow();
        Assertions.assertThat(salarySummary.amount()).isEqualTo(5000.0);
        Assertions.assertThat(salarySummary.totalTransactions()).isEqualTo(1);

        verify(jpaCategoryRepository, times(1)).findByUserIdWithSummary(userId);
    }
}
