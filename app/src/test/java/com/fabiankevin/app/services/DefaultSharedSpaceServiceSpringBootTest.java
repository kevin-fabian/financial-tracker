package com.fabiankevin.app.services;

import com.fabiankevin.app.models.*;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Invitation;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.shared_space.AcceptInvitationCommand;
import com.fabiankevin.app.services.commands.shared_space.AddSharedResourceCommand;
import com.fabiankevin.app.services.commands.shared_space.CreateSharedSpaceCommand;
import com.fabiankevin.app.services.commands.shared_space.SendInvitationCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
public class DefaultSharedSpaceServiceSpringBootTest {
    @Autowired
    private SharedSpaceService sharedSpaceService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private StatsService statsService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @DisplayName("""
            Mutual sharing mode: transactions should be combined after space acceptance
            """)
    @Test
    void mutualSharingFlow_shouldCombineTransactions() {
        UUID ownerUserId = UUID.randomUUID();
        UUID partnerUserid = UUID.randomUUID();

        // Owner transactions
        addTransaction(ownerUserId, 500);
        addTransaction(ownerUserId, 1500);
        addTransaction(ownerUserId, 3000);

        // Partner transactions
        addTransaction(partnerUserid, 500);
        addTransaction(partnerUserid, 1500);
        addTransaction(partnerUserid, 3000);

        // Step 1: Create a space
        SharedSpace initialSharedSpace = sharedSpaceService.createShare(CreateSharedSpaceCommand.builder()
                .spaceName("Partner Space")
                .ownerUserId(ownerUserId)
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .resources(List.of(AddSharedResourceCommand.builder()
                                .type(ResourceType.TRANSACTION)
                                .build(),
                        AddSharedResourceCommand.builder()
                                .type(ResourceType.BUDGET)
                                .build()))
                .build());
        UUID spaceId = initialSharedSpace.id();

        // Step 2: Invite a partner in a space
        Invitation invitation = sharedSpaceService.sendInvitation(SendInvitationCommand.builder()
                .spaceId(spaceId)
                .inviterUserId(ownerUserId)
                .inviteeEmail("partner@test.com")
                .inviteeUserId(partnerUserid)
                .proposedRole(AccessLevel.READ_WRITE)
                .build());

        // Step 3: Accept the invite
        sharedSpaceService.acceptInvitation(AcceptInvitationCommand.builder()
                .invitationId(invitation.id())
                .acceptingUserId(partnerUserid)
                .build());

        Page<Transaction> transactionsByPageQuery = transactionService.getTransactionsByPageQuery(PageQuery.withDefaults(),
                partnerUserid, null);
        List<Transaction> sharedTransactions = transactionsByPageQuery.content();

        assertEquals(6, sharedTransactions.size(), "combined transaction count");
    }

    @DisplayName("""
            Mutual sharing mode: stats should combine total balance, expenses, and income after space acceptance
            """)
    @Test
    void mutualSharingFlow_shouldCombineStats() {
        UUID ownerUserId = UUID.randomUUID();
        UUID partnerUserid = UUID.randomUUID();

        // Owner: income 10000, expenses 3000
        addIncomeTransaction(ownerUserId, 10000);
        addTransaction(ownerUserId, 1000);
        addTransaction(ownerUserId, 2000);

        // Partner: income 6000, expenses 2000
        addIncomeTransaction(partnerUserid, 6000);
        addTransaction(partnerUserid, 500);
        addTransaction(partnerUserid, 1500);

        // Step 1: Create a space
        SharedSpace initialSharedSpace = sharedSpaceService.createShare(CreateSharedSpaceCommand.builder()
                .spaceName("Partner Space")
                .ownerUserId(ownerUserId)
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .resources(List.of(AddSharedResourceCommand.builder()
                                .type(ResourceType.TRANSACTION)
                                .build(),
                        AddSharedResourceCommand.builder()
                                .type(ResourceType.BUDGET)
                                .build()))
                .build());
        UUID spaceId = initialSharedSpace.id();

        // Step 2: Invite a partner in a space
        Invitation invitation = sharedSpaceService.sendInvitation(SendInvitationCommand.builder()
                .spaceId(spaceId)
                .inviterUserId(ownerUserId)
                .inviteeEmail("partner@test.com")
                .inviteeUserId(partnerUserid)
                .proposedRole(AccessLevel.READ_WRITE)
                .build());

        // Step 3: Accept the invite
        sharedSpaceService.acceptInvitation(AcceptInvitationCommand.builder()
                .invitationId(invitation.id())
                .acceptingUserId(partnerUserid)
                .build());

        StatsQuery query = StatsQuery.builder()
                .fromDate(LocalDate.now().withDayOfMonth(1))
                .toDate(LocalDate.now())
                .build();

        var summary = statsService.getStatsSummary(ownerUserId, query);

        assertEquals(16000.0, summary.totalIncome(), 0.001, "combined income");
        assertEquals(5000.0, summary.totalExpenses(), 0.001, "combined expenses");
        assertEquals(11000.0, summary.totalBalance(), 0.001, "combined balance");
    }

    private void addTransaction(UUID userId, double amount) {
        Account account = createOrGetAccount(userId);
        Category category = createOrGetCategory(userId);

        transactionService.addTransaction(AddTransactionCommand.builder()
                .amount(Amount.of(amount))
                .description("Ramen")
                .userId(userId)
                .accountId(account.id())
                .categoryId(category.id())
                .transactionDate(LocalDate.now())
                .build());
    }

    private void addIncomeTransaction(UUID userId, double amount) {
        Account account = createOrGetAccount(userId);
        Category category = createOrGetIncomeCategory(userId);

        transactionService.addTransaction(AddTransactionCommand.builder()
                .amount(Amount.of(amount))
                .description("Salary")
                .userId(userId)
                .accountId(account.id())
                .categoryId(category.id())
                .transactionDate(LocalDate.now())
                .build());
    }

    private Category createOrGetIncomeCategory(UUID userId) {
        List<Category> categories = categoryRepository.findAllByNamesIn(List.of("Salary")).stream()
                .filter(c -> c.userId().equals(userId))
                .toList();
        if (categories.isEmpty()) {
            return categoryRepository.save(Category.builder()
                    .type(TransactionType.INCOME)
                    .name("Salary")
                    .userId(userId)
                    .icon("salary")
                    .active(true)
                    .system(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        return categories.getFirst();
    }

    private Category createOrGetCategory(UUID userId) {
        List<Category> allByNamesIn = categoryRepository.findAllByNamesIn(List.of("Food & Drinks")).stream()
                .filter(acc -> acc.userId().equals(userId))
                .toList();
        if (allByNamesIn.isEmpty()) {
            return categoryRepository.save(Category.builder()
                    .type(TransactionType.EXPENSE)
                    .name("Food & Drinks")
                    .userId(userId)
                    .icon("food")
                    .active(true)
                    .system(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        return allByNamesIn.getFirst();
    }

    private Account createOrGetAccount(UUID userId) {
        List<Account> cashWallet = accountRepository.findAllByNamesIn(List.of("Cash Wallet"))
                .stream()
                .filter(acc -> acc.userId().equals(userId))
                .toList();

        if (cashWallet.isEmpty()) {
            return accountRepository.save(Account.builder()
                    .name("Cash Wallet")
                    .type(AccountType.CASH)
                    .userId(userId)
                    .currency(Currency.getInstance("PHP"))
                    .active(true)
                    .system(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        return cashWallet.getFirst();
    }

}
