package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.BudgetAlreadyExistException;
import com.fabiankevin.app.exceptions.BudgetNotFoundException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.budgets.Budget;
import com.fabiankevin.app.models.budgets.BudgetPeriod;
import com.fabiankevin.app.models.budgets.BudgetSummary;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.BudgetRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.services.commands.budgets.CreateBudgetCommand;
import com.fabiankevin.app.services.commands.budgets.PatchBudgetCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private HouseholdService householdService;

    @InjectMocks
    private DefaultBudgetService budgetService;

    private UUID userId;
    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        category = Category.builder()
                .id(categoryId)
                .name("GROCERIES")
                .type(TransactionType.EXPENSE)
                .userId(userId)
                .icon("local_grocery_store")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    class CreateBudget {
        @Test
        void givenValidCommand_thenCreatesAndReturnsBudgetSummary() {
            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            Budget budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .user(User.of(userId))
                    .updatedBy(User.of(userId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(budgetRepository.existsByCategoryIdAndUserId(categoryId, userId)).thenReturn(false);
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(budgetRepository.save(any())).thenAnswer(invocation -> {
                Budget original = invocation.getArgument(0);
                UUID generatedId = UUID.randomUUID();
                Budget saved = Budget.builder()
                        .id(generatedId)
                        .user(original.user())
                        .updatedBy(original.updatedBy())
                        .period(original.period())
                        .category(original.category())
                        .allocated(original.allocated())
                        .createdAt(original.createdAt())
                        .updatedAt(original.updatedAt())
                        .build();
                when(budgetRepository.findBudgetSummaryById(generatedId)).thenReturn(Optional.of(
                        BudgetSummary.builder()
                                .budget(saved)
                                .spent(200.0)
                                .spentPercentage(40.0)
                                .build()
                ));
                return saved;
            });
            when(userClient.getUsersByIds(List.of(userId))).thenReturn(
                    List.of(User.builder().id(userId).firstName("John").lastName("Doe").build())
            );

            BudgetSummary created = budgetService.createBudget(command);

            assertNotNull(created.budget().id(), "id should be generated");
            assertEquals(userId, created.budget().user().id(), "user should match command");
            assertEquals(BudgetPeriod.MONTHLY, created.budget().period(), "period should match command");
            assertEquals(categoryId, created.budget().category().id(), "categoryId should match command");
            assertEquals("GROCERIES", created.budget().category().name(), "categoryName should be resolved from category");
            assertEquals("local_grocery_store", created.budget().category().icon(), "categoryIcon should be resolved from category");
            assertEquals(500.0, created.budget().allocated(), "allocated should match command");
            assertEquals(200.0, created.spent(), "spent should reflect summed transactions for the category");
            assertEquals(40.0, created.spentPercentage(), "spentPercentage should be spent/allocated*100");

            verify(categoryRepository, times(1)).findById(categoryId);
            verify(budgetRepository, times(1)).save(any());
            verify(budgetRepository, times(1)).findBudgetSummaryById(any());
            verify(userClient, times(1)).getUsersByIds(List.of(userId));
        }

        @Test
        void givenCategoryNotFound_thenThrowsCategoryNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.createBudget(command))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, times(1)).findById(categoryId);
            verify(budgetRepository, never()).save(any());
        }

        @Test
        void givenCategoryBelongsToDifferentUser_thenThrowsCategoryNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Category otherUserCategory = Category.builder()
                    .id(categoryId)
                    .name("GROCERIES")
                    .type(TransactionType.EXPENSE)
                    .userId(otherUserId)
                    .system(false)
                    .icon("local_grocery_store")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(otherUserCategory));

            assertThatThrownBy(() -> budgetService.createBudget(command))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, times(1)).findById(categoryId);
            verify(budgetRepository, never()).save(any());
        }

        @Test
        void givenSystemCategoryBelongsToDifferentUser_thenCreatesBudgetSuccessfully() {
            UUID userId = UUID.randomUUID();
            UUID systemUserId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Category systemCategory = Category.builder()
                    .id(categoryId)
                    .name("SYSTEM_CATEGORY")
                    .type(TransactionType.EXPENSE)
                    .userId(systemUserId)
                    .system(true)
                    .icon("system")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            Budget budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .user(User.of(userId))
                    .updatedBy(User.of(userId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(systemCategory)
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(budgetRepository.existsByCategoryIdAndUserId(categoryId, userId)).thenReturn(false);
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(systemCategory));
            when(budgetRepository.save(any())).thenAnswer(invocation -> {
                Budget original = invocation.getArgument(0);
                UUID generatedId = UUID.randomUUID();
                Budget saved = Budget.builder()
                        .id(generatedId)
                        .user(original.user())
                        .updatedBy(original.updatedBy())
                        .period(original.period())
                        .category(original.category())
                        .allocated(original.allocated())
                        .createdAt(original.createdAt())
                        .updatedAt(original.updatedAt())
                        .build();
                when(budgetRepository.findBudgetSummaryById(generatedId)).thenReturn(Optional.of(
                        BudgetSummary.builder()
                                .budget(saved)
                                .spent(0.0)
                                .spentPercentage(0.0)
                                .build()
                ));
                return saved;
            });
            when(userClient.getUsersByIds(List.of(userId))).thenReturn(
                    List.of(User.builder().id(userId).firstName("John").lastName("Doe").build())
            );

            BudgetSummary created = budgetService.createBudget(command);

            assertNotNull(created.budget().id(), "id should be generated");
            assertEquals(userId, created.budget().user().id(), "user should match command");
            assertEquals(categoryId, created.budget().category().id(), "categoryId should match command");
            assertEquals("SYSTEM_CATEGORY", created.budget().category().name(), "system category name should be resolved");

            verify(categoryRepository, times(1)).findById(categoryId);
            verify(budgetRepository, times(1)).save(any());
            verify(budgetRepository, times(1)).findBudgetSummaryById(any());
            verify(userClient, times(1)).getUsersByIds(List.of(userId));
        }

        @Test
        void givenCategoryAlreadyHasBudgetThisMonth_thenThrowsBudgetAlreadyExistException() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            CreateBudgetCommand command = CreateBudgetCommand.builder()
                    .userId(userId)
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(categoryId)
                    .allocated(500.0)
                    .build();

            when(budgetRepository.existsByCategoryIdAndUserId(categoryId, userId))
                    .thenReturn(true);

            assertThatThrownBy(() -> budgetService.createBudget(command))
                    .isInstanceOf(BudgetAlreadyExistException.class);

            verify(budgetRepository, times(1)).existsByCategoryIdAndUserId(categoryId, userId);
            verify(categoryRepository, never()).findById(any());
            verify(budgetRepository, never()).save(any());
        }
    }

    @Nested
    class GetBudgetsByUserId {
        @Test
        void givenExistingBudgets_thenReturnsSummaries() {
            UUID userId = UUID.randomUUID();
            UUID budgetId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("GROCERIES")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .icon("local_grocery_store")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Budget budget = Budget.builder()
                    .id(budgetId)
                    .user(User.of(userId))
                    .updatedBy(User.of(userId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            BudgetSummary summary = BudgetSummary.builder()
                    .budget(budget)
                    .spent(200.0)
                    .spentPercentage(40.0)
                    .build();

            when(budgetRepository.findAllBudgetSummaryByUserId(eq(List.of(userId)), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(summary));
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            List<BudgetSummary> results = budgetService.getBudgetsByUserId(userId);

            assertThat(results).hasSize(1);
            BudgetSummary result = results.getFirst();

            // identity & ownership fields preserved from nested budget
            assertEquals(budgetId, result.budget().id(), "id should be preserved");
            assertEquals(userId, result.budget().user().id(), "user should be preserved");

            // timestamps & period preserved from nested budget
            assertNotNull(result.budget().createdAt(), "createdAt should be preserved from source budget");
            assertNotNull(result.budget().updatedAt(), "updatedAt should be preserved from source budget");
            assertEquals(BudgetPeriod.MONTHLY, result.budget().period(), "period should be preserved");

            // category fields preserved from nested budget
            assertEquals(categoryId, result.budget().category().id(), "categoryId should be preserved");
            assertEquals("GROCERIES", result.budget().category().name(), "categoryName should be preserved");
            assertEquals("local_grocery_store", result.budget().category().icon(), "categoryIcon should be preserved");

            // amount fields
            assertEquals(500.0, result.budget().allocated(), "allocated should be preserved");
            assertEquals(200.0, result.spent(), "spent should be preserved");
            assertEquals(40.0, result.spentPercentage(), "spentPercentage should be preserved");

            verify(budgetRepository, times(1)).findAllBudgetSummaryByUserId(eq(List.of(userId)), any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        void givenNoBudgets_thenReturnsEmptyList() {
            UUID userId = UUID.randomUUID();

            when(budgetRepository.findAllBudgetSummaryByUserId(eq(List.of(userId)), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(householdService.getHouseholdMembersUserIds(userId)).thenReturn(List.of(userId));

            List<BudgetSummary> results = budgetService.getBudgetsByUserId(userId);

            assertThat(results).isEmpty();
            verify(budgetRepository, times(1)).findAllBudgetSummaryByUserId(eq(List.of(userId)), any(LocalDate.class), any(LocalDate.class));
        }
    }

    @Nested
    class PatchBudget {
        @Test
        void givenValidCommand_thenUpdatesProvidedFieldsAndReturnsBudgetSummary() {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();
            UUID newCategoryId = UUID.randomUUID();
            Category newCategory = Category.builder()
                    .id(newCategoryId)
                    .name("RENT")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .icon("home")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Budget existing = Budget.builder()
                    .id(id)
                    .user(User.of(userId))
                    .updatedBy(User.of(userId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(Category.builder()
                            .id(UUID.randomUUID())
                            .name("GROCERIES")
                            .type(TransactionType.EXPENSE)
                            .userId(userId)
                            .icon("local_grocery_store")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build())
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            PatchBudgetCommand command = PatchBudgetCommand.builder()
                    .id(id)
                    .categoryId(newCategoryId)
                    .period(BudgetPeriod.YEARLY)
                    .allocated(1000.0)
                    .userId(userId)
                    .build();

            when(budgetRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
            when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));
            when(budgetRepository.save(any())).thenAnswer(invocation -> {
                Budget original = invocation.getArgument(0);
                Budget saved = Budget.builder()
                        .id(original.id())
                        .user(original.user())
                        .updatedBy(original.updatedBy())
                        .period(original.period())
                        .category(original.category())
                        .allocated(original.allocated())
                        .createdAt(original.createdAt())
                        .updatedAt(original.updatedAt())
                        .build();
                when(budgetRepository.findBudgetSummaryById(original.id())).thenReturn(Optional.of(
                        BudgetSummary.builder()
                                .budget(saved)
                                .spent(200.0)
                                .spentPercentage(20.0)
                                .build()
                ));
                return saved;
            });
            when(userClient.getUsersByIds(List.of(userId))).thenReturn(
                    List.of(User.builder().id(userId).firstName("John").lastName("Doe").build())
            );

            BudgetSummary updated = budgetService.patchBudget(command);

            // identity & ownership fields
            assertEquals(id, updated.budget().id(), "id should be preserved");
            assertEquals(userId, updated.budget().user().id(), "user should be preserved");
            assertEquals(userId, updated.budget().updatedBy().id(), "updatedBy should be set");

            // timestamps
            assertNotNull(updated.budget().createdAt(), "createdAt should be preserved from existing budget");
            assertNotNull(updated.budget().updatedAt(), "updatedAt should not be null");

            // period & category fields
            assertEquals(BudgetPeriod.YEARLY, updated.budget().period(), "period should be updated");
            assertEquals(newCategoryId, updated.budget().category().id(), "categoryId should be updated");
            assertEquals("RENT", updated.budget().category().name(), "categoryName should be updated");
            assertEquals("home", updated.budget().category().icon(), "categoryIcon should be updated");

            // amount fields
            assertEquals(1000.0, updated.budget().allocated(), "allocated should be updated");
            assertEquals(200.0, updated.spent(), "spent should reflect summed transactions");
            assertEquals(20.0, updated.spentPercentage(), "spentPercentage should be 200/1000*100");

            verify(budgetRepository, times(1)).findByIdAndUserId(id, userId);
            verify(categoryRepository, times(1)).findById(newCategoryId);
            verify(budgetRepository, times(1)).save(any());
            verify(budgetRepository, times(1)).findBudgetSummaryById(any());
            verify(userClient, times(1)).getUsersByIds(List.of(userId));
        }

        @Test
        void givenBudgetNotFound_thenThrowsBudgetNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID id = UUID.randomUUID();

            PatchBudgetCommand command = PatchBudgetCommand.builder()
                    .id(id)
                    .userId(userId)
                    .build();

            when(budgetRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.patchBudget(command))
                    .isInstanceOf(BudgetNotFoundException.class);

            verify(budgetRepository, times(1)).findByIdAndUserId(id, userId);
            verify(budgetRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteBudget {
        @Test
        void givenValidIdAndUserId_thenDeletesBudget() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            budgetService.deleteBudgetById(id, userId);

            verify(budgetRepository, times(1)).deleteByIdAndUserId(id, userId);
        }
    }

    @Nested
    class GetBudgetSummaryByIdAndUserId {
        @Test
        void givenExistingBudgetWithMatchingUserId_thenReturnsEnrichedSummary() {
            UUID userId = UUID.randomUUID();
            UUID budgetId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();

            Category category = Category.builder()
                    .id(categoryId)
                    .name("GROCERIES")
                    .type(TransactionType.EXPENSE)
                    .userId(userId)
                    .icon("local_grocery_store")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Budget budget = Budget.builder()
                    .id(budgetId)
                    .user(User.of(userId))
                    .updatedBy(User.of(userId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            BudgetSummary summary = BudgetSummary.builder()
                    .budget(budget)
                    .spent(200.0)
                    .spentPercentage(40.0)
                    .build();

            when(budgetRepository.findBudgetSummaryById(budgetId)).thenReturn(Optional.of(summary));
            User enrichedUser = User.builder()
                    .id(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .build();
            when(userClient.getUsersByIds(List.of(userId))).thenReturn(List.of(enrichedUser));

            BudgetSummary result = budgetService.getBudgetSummaryByIdAndUserId(budgetId, userId);

            assertEquals(budgetId, result.budget().id(), "id should match");
            assertEquals(userId, result.budget().user().id(), "user should match");
            assertEquals("John", result.budget().user().firstName(), "firstName should be enriched");
            assertEquals("Doe", result.budget().user().lastName(), "lastName should be enriched");
            assertEquals(500.0, result.budget().allocated(), "allocated should match");
            assertEquals(200.0, result.spent(), "spent should match");
            assertEquals(40.0, result.spentPercentage(), "spentPercentage should match");

            verify(budgetRepository, times(1)).findBudgetSummaryById(budgetId);
            verify(userClient, times(1)).getUsersByIds(List.of(userId));
        }

        @Test
        void givenExistingBudgetWithDifferentUserId_thenThrowsBudgetNotFoundException() {
            UUID budgetId = UUID.randomUUID();
            UUID correctUserId = UUID.randomUUID();
            UUID wrongUserId = UUID.randomUUID();

            Category category = Category.builder()
                    .id(UUID.randomUUID())
                    .name("GROCERIES")
                    .type(TransactionType.EXPENSE)
                    .userId(correctUserId)
                    .icon("local_grocery_store")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Budget budget = Budget.builder()
                    .id(budgetId)
                    .user(User.of(correctUserId))
                    .updatedBy(User.of(correctUserId))
                    .period(BudgetPeriod.MONTHLY)
                    .category(category)
                    .allocated(500.0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            BudgetSummary summary = BudgetSummary.builder()
                    .budget(budget)
                    .spent(200.0)
                    .spentPercentage(40.0)
                    .build();

            when(budgetRepository.findBudgetSummaryById(budgetId)).thenReturn(Optional.of(summary));

            assertThatThrownBy(() -> budgetService.getBudgetSummaryByIdAndUserId(budgetId, wrongUserId))
                    .isInstanceOf(BudgetNotFoundException.class);

            verify(budgetRepository, times(1)).findBudgetSummaryById(budgetId);
        }
    }
}
