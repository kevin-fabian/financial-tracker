package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.events.EventPublisher;
import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.exceptions.CategoryNotFoundException;
import com.fabiankevin.app.exceptions.DailyTransactionLimitExceededException;
import com.fabiankevin.app.exceptions.TransactionNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.SummarySeries;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.HouseholdRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.services.queries.SummaryQuery;
import com.fabiankevin.app.services.summaries.SummaryGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTransactionServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    private final SummaryGenerator categorySummaryGenerator = mock(SummaryGenerator.class);
    @Mock
    private HouseholdRepository householdRepository;
    @Mock
    private EventPublisher<Transaction> eventPublisher;
    @Mock
    private UserClient userClient;
    private DefaultTransactionService transactionService;

    @BeforeEach
    void setup() {
        when(categorySummaryGenerator.supports()).thenReturn(SummaryType.CATEGORY);
        List<SummaryGenerator> summaryGenerators = List.of(categorySummaryGenerator);
        transactionService = new DefaultTransactionService(
                accountRepository,
                categoryRepository,
                transactionRepository,
                summaryGenerators,
                householdRepository,
                eventPublisher,
                100,
                userClient
        );
    }

   @Nested
   class AddTransaction {
       @Test
       void givenValidCommand_thenShouldSucceed() {
           UUID userId = UUID.randomUUID();
           AddTransactionCommand command = AddTransactionCommand.builder()
                   .userId(userId)
                   .amount(100)
                   .accountId(UUID.randomUUID())
                   .description("Food and drinks")
                   .categoryId(UUID.randomUUID())
                   .transactionDate(LocalDate.now())
                   .build();
           when(accountRepository.findById(command.accountId())).thenReturn(Optional.ofNullable(Account.builder()
                   .id(command.accountId())
                   .name("GCASH")
                   .currency(Currency.getInstance("PHP"))
                   .user(User.of(userId))
                   .build()));
           when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(Category.builder()
                   .id(command.categoryId())
                   .name("FOOD")
                   .type(TransactionType.EXPENSE)
                   .userId(userId)
                   .build()));
           when(transactionRepository.save(any())).then(invocationOnMock -> {
               Transaction transaction = invocationOnMock.getArgument(0);

               return transaction.toBuilder()
                       .id(UUID.randomUUID())
                       .build();
           });

           Transaction transaction = transactionService.addTransaction(command);

           assertEquals("Food and drinks", transaction.description(), "description should match");

           verify(transactionRepository, times(1)).countByUserIdAndCreatedAtOnDate(userId, LocalDate.now());
           verify(accountRepository, times(1)).findById(command.accountId());
           verify(categoryRepository, times(1)).findById(command.categoryId());
           verify(transactionRepository, times(1)).save(any());
       }

       @Test
       void givenAccountNotFound_thenShouldThrow() {
           UUID userId = UUID.randomUUID();
           AddTransactionCommand command = AddTransactionCommand.builder()
                   .userId(userId)
                   .amount(100)
                   .accountId(UUID.randomUUID())
                   .description("Food and drinks")
                   .categoryId(UUID.randomUUID())
                   .transactionDate(LocalDate.now())
                   .build();
           when(accountRepository.findById(command.accountId())).thenReturn(Optional.empty());

           assertThrows(AccountNotFoundException.class, () -> transactionService.addTransaction(command));
           verify(transactionRepository, times(1)).countByUserIdAndCreatedAtOnDate(userId, LocalDate.now());
           verify(accountRepository, times(1)).findById(command.accountId());
           verify(categoryRepository, never()).findById(any());
           verify(transactionRepository, never()).save(any());
       }

       @Test
       void givenAccountBelongsToAnotherUser_thenShouldThrow() {
           UUID userId = UUID.randomUUID();
           UUID otherUserId = UUID.randomUUID();
           AddTransactionCommand command = AddTransactionCommand.builder()
                   .userId(userId)
                   .amount(100)
                   .accountId(UUID.randomUUID())
                   .description("Food and drinks")
                   .categoryId(UUID.randomUUID())
                   .transactionDate(LocalDate.now())
                   .build();
           when(accountRepository.findById(command.accountId())).thenReturn(Optional.of(Account.builder()
                   .id(command.accountId())
                   .name("GCASH")
                   .currency(Currency.getInstance("PHP"))
                   .user(User.of(otherUserId))
                   .build()));

           assertThrows(AccountNotFoundException.class, () -> transactionService.addTransaction(command));
           verify(transactionRepository, times(1)).countByUserIdAndCreatedAtOnDate(userId, LocalDate.now());
           verify(accountRepository, times(1)).findById(command.accountId());
           verify(categoryRepository, never()).findById(any());
           verify(transactionRepository, never()).save(any());
       }

       @Test
       void givenCategoryNotFound_thenShouldThrow() {
           UUID userId = UUID.randomUUID();
           AddTransactionCommand command = AddTransactionCommand.builder()
                   .userId(userId)
                   .amount(100)
                   .accountId(UUID.randomUUID())
                   .description("Food and drinks")
                   .categoryId(UUID.randomUUID())
                   .transactionDate(LocalDate.now())
                   .build();
           when(accountRepository.findById(command.accountId())).thenReturn(Optional.of(Account.builder()
                   .id(command.accountId())
                   .name("GCASH")
                   .currency(Currency.getInstance("PHP"))
                   .user(User.of(userId))
                   .build()));
           when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.empty());

           assertThrows(CategoryNotFoundException.class, () -> transactionService.addTransaction(command));
           verify(transactionRepository, times(1)).countByUserIdAndCreatedAtOnDate(userId, LocalDate.now());
           verify(accountRepository, times(1)).findById(command.accountId());
           verify(categoryRepository, times(1)).findById(command.categoryId());
           verify(transactionRepository, never()).save(any());
       }

       @Test
       void givenCategoryBelongsToAnotherUser_thenShouldThrow() {
           UUID userId = UUID.randomUUID();
           UUID otherUserId = UUID.randomUUID();
           AddTransactionCommand command = AddTransactionCommand.builder()
                   .userId(userId)
                   .amount(100)
                   .accountId(UUID.randomUUID())
                   .description("Food and drinks")
                   .categoryId(UUID.randomUUID())
                   .transactionDate(LocalDate.now())
                   .build();
           when(accountRepository.findById(command.accountId())).thenReturn(Optional.of(Account.builder()
                   .id(command.accountId())
                   .name("GCASH")
                   .currency(Currency.getInstance("PHP"))
                   .user(User.of(userId))
                   .build()));
           when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(Category.builder()
                   .id(command.categoryId())
                   .name("FOOD")
                   .type(TransactionType.EXPENSE)
                   .userId(otherUserId)
                   .build()));

           assertThrows(CategoryNotFoundException.class, () -> transactionService.addTransaction(command));
           verify(transactionRepository, times(1)).countByUserIdAndCreatedAtOnDate(userId, LocalDate.now());
           verify(accountRepository, times(1)).findById(command.accountId());
           verify(categoryRepository, times(1)).findById(command.categoryId());
           verify(transactionRepository, never()).save(any());
       }

       @Test
       void givenUserHasSharedSpace_thenShouldPublishEvent() {
           UUID userId = UUID.randomUUID();
           UUID sharedSpaceId = UUID.randomUUID();
           AddTransactionCommand command = AddTransactionCommand.builder()
                   .userId(userId)
                   .amount(100)
                   .accountId(UUID.randomUUID())
                   .description("Food and drinks")
                   .categoryId(UUID.randomUUID())
                   .transactionDate(LocalDate.now())
                   .build();
           when(accountRepository.findById(command.accountId())).thenReturn(Optional.of(Account.builder()
                   .id(command.accountId())
                   .name("GCASH")
                   .currency(Currency.getInstance("PHP"))
                   .user(User.of(userId))
                   .build()));
           when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(Category.builder()
                   .id(command.categoryId())
                   .name("FOOD")
                   .type(TransactionType.EXPENSE)
                   .userId(userId)
                   .build()));
           when(transactionRepository.save(any())).then(invocation -> invocation.getArgument(0));
           Household household = mock(Household.class);
           when(household.id()).thenReturn(sharedSpaceId);
           when(householdRepository.findByUserId(userId)).thenReturn(Optional.of(household));

           Transaction transaction = transactionService.addTransaction(command);

           assertEquals("Food and drinks", transaction.description());
           verify(transactionRepository, times(1)).countByUserIdAndCreatedAtOnDate(userId, LocalDate.now());
           verify(householdRepository, times(1)).findByUserId(userId);
           verify(eventPublisher, times(1)).publish(eq(sharedSpaceId), any());
       }

       @Test
       void givenUserHasNoSharedSpace_thenShouldNotPublishEvent() {
           UUID userId = UUID.randomUUID();
           AddTransactionCommand command = AddTransactionCommand.builder()
                   .userId(userId)
                   .amount(100)
                   .accountId(UUID.randomUUID())
                   .description("Food and drinks")
                   .categoryId(UUID.randomUUID())
                   .transactionDate(LocalDate.now())
                   .build();
           when(accountRepository.findById(command.accountId())).thenReturn(Optional.of(Account.builder()
                   .id(command.accountId())
                   .name("GCASH")
                   .currency(Currency.getInstance("PHP"))
                   .user(User.of(userId))
                   .build()));
           when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(Category.builder()
                   .id(command.categoryId())
                   .name("FOOD")
                   .type(TransactionType.EXPENSE)
                   .userId(userId)
                   .build()));
           when(transactionRepository.save(any())).then(invocation -> invocation.getArgument(0));
           when(householdRepository.findByUserId(userId)).thenReturn(Optional.empty());

           Transaction transaction = transactionService.addTransaction(command);

           assertEquals("Food and drinks", transaction.description());
           verify(transactionRepository, times(1)).countByUserIdAndCreatedAtOnDate(userId, LocalDate.now());
           verify(householdRepository, times(1)).findByUserId(userId);
           verify(eventPublisher, never()).publish(any(), any());
       }

       @Test
       void givenDailyLimitReachedOnCurrentDay_thenShouldThrow() {
           UUID userId = UUID.randomUUID();
           LocalDate today = LocalDate.now();
           AddTransactionCommand command = AddTransactionCommand.builder()
                   .userId(userId)
                   .amount(100)
                   .accountId(UUID.randomUUID())
                   .description("Food and drinks")
                   .categoryId(UUID.randomUUID())
                   .transactionDate(today)
                   .build();
           when(transactionRepository.countByUserIdAndCreatedAtOnDate(userId, today)).thenReturn(100L);

           assertThrows(DailyTransactionLimitExceededException.class, () -> transactionService.addTransaction(command));
           verify(transactionRepository, times(1)).countByUserIdAndCreatedAtOnDate(userId, today);
           verify(accountRepository, never()).findById(any());
           verify(categoryRepository, never()).findById(any());
           verify(transactionRepository, never()).save(any());
       }
   }

    @Test
    void patchTransaction_givenValidCommand_thenShouldUpdate() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID newAccountId = UUID.randomUUID();
        UUID newCategoryId = UUID.randomUUID();

        Transaction existing = Transaction.builder()
                .id(transactionId)
                .account(Account.builder().id(UUID.randomUUID()).user(User.of(userId)).name("GCASH").currency(java.util.Currency.getInstance("PHP")).build())
                .category(Category.builder().id(UUID.randomUUID()).type(TransactionType.EXPENSE).userId(userId).name("FOOD").build())
                .type(TransactionType.EXPENSE)
                .amount(100)
                .description("old")
                .transactionDate(LocalDate.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .addedBy(User.of(userId))
                .updatedBy(User.of(userId))
                .build();

        PatchTransactionCommand command = PatchTransactionCommand.builder()
                .id(transactionId)
                .accountId(newAccountId)
                .categoryId(newCategoryId)
                .description("updated")
                .userId(userId)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existing));
        when(accountRepository.findById(newAccountId)).thenReturn(Optional.of(Account.builder().id(newAccountId).user(User.of(userId)).name("NEW").currency(java.util.Currency.getInstance("PHP")).build()));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(Category.builder().id(newCategoryId).type(TransactionType.EXPENSE).userId(userId).name("NEWCAT").build()));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction updated = transactionService.patchTransaction(command);

        assertEquals("updated", updated.description());
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(accountRepository, times(1)).findById(newAccountId);
        verify(categoryRepository, times(1)).findById(newCategoryId);
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void patchTransaction_givenNonExisting_thenShouldThrow() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        PatchTransactionCommand command = PatchTransactionCommand.builder()
                .id(transactionId)
                .description("updated")
                .userId(userId)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.patchTransaction(command));
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteTransaction_givenExisting_thenShouldDelete() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.deleteByIdAndUserId(transactionId, userId)).thenReturn(1);

        transactionService.deleteTransaction(transactionId, userId);

        verify(transactionRepository, times(1)).deleteByIdAndUserId(transactionId, userId);
    }

    @Test
    void getTransactionById_givenExistingAndBelongsToUser_thenShouldReturnResponse() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Transaction tx = Transaction.builder()
                .id(transactionId)
                .account(Account.builder().id(UUID.randomUUID()).user(User.of(userId)).name("ACCT").currency(java.util.Currency.getInstance("PHP")).build())
                .category(Category.builder().id(UUID.randomUUID()).type(TransactionType.EXPENSE).userId(userId).name("CAT").build())
                .type(TransactionType.EXPENSE)
                .amount(100)
                .description("desc")
                .transactionDate(LocalDate.now())
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .addedBy(User.of(userId))
                .updatedBy(User.of(userId))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(tx));

        com.fabiankevin.app.web.controllers.dtos.TransactionResponse response = transactionService.getTransactionById(transactionId, userId);

        assertEquals(transactionId, response.id());
        assertEquals("desc", response.description());
        verify(transactionRepository, times(1)).findById(transactionId);
    }

    @Test
    void getTransactionById_givenNonExistingOrNotBelongToUser_thenShouldThrow() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransactionById(transactionId, userId));
        verify(transactionRepository, times(1)).findById(transactionId);
    }

    @Test
    void getSummary_givenValidType_thenShouldReturnSummarySeries() {
        SummaryQuery query = SummaryQuery.builder()
                .type(SummaryType.CATEGORY)
                .from(LocalDate.now().minusMonths(1))
                .to(LocalDate.now())
                .build();

        List<SummaryPoint> expectedPoints = List.of(
                new SummaryPoint("FOOD", 500.0),
                new SummaryPoint("TRANSPORT", 200.0)
        );
        when(categorySummaryGenerator.generate(query)).thenReturn(expectedPoints);

        SummarySeries result = transactionService.getSummary(query);

        assertEquals(SummaryType.CATEGORY, result.type());
        assertEquals(2, result.points().size());
        assertEquals("FOOD", result.points().get(0).label());
        assertEquals(500.0, result.points().get(0).total());
        verify(categorySummaryGenerator, times(1)).generate(query);
    }

    @Test
    void getSummary_givenNoGeneratorSummeryType_thenShouldThrow() {
        SummaryQuery query = SummaryQuery.builder()
                .type(SummaryType.YEARLY)
                .from(LocalDate.now().minusMonths(1))
                .to(LocalDate.now())
                .build();

        assertThrows(IllegalArgumentException.class, () -> transactionService.getSummary(query));
    }

    @Test
    void getSummary_givenEmptyGeneratorList_thenShouldThrow() {
        SummaryQuery query = SummaryQuery.builder()
                .type(SummaryType.MONTHLY)
                .build();

        assertThrows(IllegalArgumentException.class, () -> transactionService.getSummary(query));
    }

    @Test
    void getTransactionsByPageQuery_givenTypeFilter_thenShouldCallRepositoryWithType() {
        UUID userId = UUID.randomUUID();
        PageQuery query = new PageQuery(0, 10, "transactionDate", "DESC");
        TransactionType type = TransactionType.EXPENSE;

        Page<Transaction> expectedPage = Page.<Transaction>builder()
                .content(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .first(true)
                .build();

        when(transactionRepository.getTransactionsByPageAndUserIdAndType(query, Set.of(userId), type)).thenReturn(expectedPage);

        Page<Transaction> result = transactionService.getTransactionsByPageQuery(query, userId, type);

        assertEquals(expectedPage, result);
        verify(transactionRepository, times(1)).getTransactionsByPageAndUserIdAndType(query, Set.of(userId), type);
        verify(transactionRepository, never()).getTransactionsByPageAndUserId(any(), any());
    }

    @Test
    void getTransactionsByPageQuery_givenNullType_thenShouldCallRepositoryWithoutType() {
        UUID userId = UUID.randomUUID();
        PageQuery query = new PageQuery(0, 20, "totalAmount", "ASC");

        Page<Transaction> expectedPage = Page.<Transaction>builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(5)
                .totalPages(1)
                .last(true)
                .first(true)
                .build();

        when(transactionRepository.getTransactionsByPageAndUserIdAndType(query, Set.of(userId), null)).thenReturn(expectedPage);

        Page<Transaction> result = transactionService.getTransactionsByPageQuery(query, userId, null);

        assertEquals(expectedPage, result);
        verify(transactionRepository, times(1)).getTransactionsByPageAndUserIdAndType(query, Set.of(userId), null);
    }

    @Test
    void getTransactionsByPageQuery_givenTypeFilterWithResults_thenShouldReturnPaginatedTransactions() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        PageQuery query = new PageQuery(1, 5, "transactionDate", "ASC");
        TransactionType type = TransactionType.INCOME;

        Transaction tx = Transaction.builder()
                .id(transactionId)
                .account(Account.builder().id(UUID.randomUUID()).user(User.of(userId)).name("ACCT").currency(java.util.Currency.getInstance("PHP")).build())
                .category(Category.builder().id(UUID.randomUUID()).type(TransactionType.INCOME).userId(userId).name("SALARY").build())
                .type(TransactionType.INCOME)
                .amount(5000)
                .description("Salary")
                .transactionDate(LocalDate.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .addedBy(User.of(userId))
                .updatedBy(User.of(userId))
                .build();

        Page<Transaction> expectedPage = Page.<Transaction>builder()
                .content(List.of(tx))
                .page(1)
                .size(5)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .first(true)
                .build();

        when(transactionRepository.getTransactionsByPageAndUserIdAndType(query, Set.of(userId), type)).thenReturn(expectedPage);

        Page<Transaction> result = transactionService.getTransactionsByPageQuery(query, userId, type);

        assertEquals(1, result.content().size());
        assertEquals(transactionId, result.content().get(0).id());
        verify(transactionRepository, times(1)).getTransactionsByPageAndUserIdAndType(query, Set.of(userId), type);
    }
}