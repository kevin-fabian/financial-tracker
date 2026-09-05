package com.fabiankevin.app.web.controllers.helper;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.TransactionService;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class TransactionServiceTestHelper {
    private static final String EXPENSE_CATEGORY_NAME = "Food";
    private static final String INCOME_CATEGORY_NAME = "Salary";
    private static final String ACCOUNT_NAME = "Cash";

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public Transaction createTransaction(UUID userId, TransactionType type, double amount) {
        return createTransaction(userId, type, amount, LocalDate.now());
    }

    public Transaction createTransaction(UUID userId, TransactionType type, double amount, LocalDate transactionDate) {
        return createTransaction(userId, type, amount, null, transactionDate);
    }

    public Transaction createTransaction(UUID userId, TransactionType type, double amount, String categoryName) {
        return createTransaction(userId, type, amount, categoryName, LocalDate.now());
    }

    public Transaction createTransaction(UUID userId, TransactionType type, double amount, String categoryName, LocalDate transactionDate) {
        String resolvedCategory = categoryName != null ? categoryName : (type == TransactionType.EXPENSE ? EXPENSE_CATEGORY_NAME : INCOME_CATEGORY_NAME);
        String icon = categoryName != null ? resolvedCategory.toLowerCase() : (type == TransactionType.EXPENSE ? "food" : "salary");
        Category category = getOrCreateCategory(userId, resolvedCategory, type, icon);
        Account account = getOrCreateAccount(userId);

        return transactionService.addTransaction(AddTransactionCommand.builder()
                .amount(amount)
                .transactionDate(transactionDate)
                .categoryId(category.id())
                .accountId(account.id())
                .userId(userId)
                .build());
    }

    private Category getOrCreateCategory(UUID userId, String name, TransactionType type, String icon) {
        return findCategory(userId, name, type)
                .orElseGet(() -> categoryService.createCategory(CreateCategoryCommand.builder()
                        .name(name)
                        .type(type)
                        .icon(icon)
                        .userId(userId)
                        .build()));
    }

    private Optional<Category> findCategory(UUID userId, String name, TransactionType type) {
        return categoryRepository.findByNameAndTypeAndUserId(name, type, userId);
    }

    private Account getOrCreateAccount(UUID userId) {
        return findAccount(userId, ACCOUNT_NAME, AccountType.CASH)
                .orElseGet(() -> accountService.createAccount(CreateAccountCommand.builder()
                        .name(ACCOUNT_NAME)
                        .currency(Currency.getInstance("USD"))
                        .type(AccountType.CASH)
                        .userId(userId)
                        .build()));
    }

    private Optional<Account> findAccount(UUID userId, String name, AccountType type) {
        return accountRepository.findByNameAndTypeAndUserId(name, type, userId);
    }
}
