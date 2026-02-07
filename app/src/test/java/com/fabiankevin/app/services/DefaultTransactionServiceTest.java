package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.TransactionNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Amount;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTransactionServiceTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @InjectMocks
    private DefaultTransactionService transactionService;

    @Test
    void addTransaction_givenValidCommand_thenShouldSucceed() {
        UUID userId = UUID.randomUUID();
        AddTransactionCommand command = AddTransactionCommand.builder()
                .userId(userId)
                .amount(Amount.of(100, Currency.getInstance("PHP")))
                .accountId(UUID.randomUUID())
                .description("Food and drinks")
                .type(TransactionType.EXPENSE)
                .categoryId(UUID.randomUUID())
                .transactionDate(LocalDate.now())
                .build();
        when(accountRepository.findById(command.accountId())).thenReturn(Optional.ofNullable(Account.builder()
                        .id(command.accountId())
                        .name("GCASH")
                        .currency(Currency.getInstance("PHP"))
                        .userId(userId)
                .build()));
        when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(Category.builder()
                        .id(command.categoryId())
                        .name("FOOD")
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

        verify(accountRepository, times(1)).findById(command.accountId());
        verify(categoryRepository, times(1)).findById(command.categoryId());
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void patchTransaction_givenValidCommand_thenShouldUpdate() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID newAccountId = UUID.randomUUID();
        UUID newCategoryId = UUID.randomUUID();

        Transaction existing = Transaction.builder()
                .id(transactionId)
                .account(Account.builder().id(UUID.randomUUID()).userId(userId).name("GCASH").currency(java.util.Currency.getInstance("PHP")).build())
                .category(Category.builder().id(UUID.randomUUID()).userId(userId).name("FOOD").build())
                .type(TransactionType.EXPENSE)
                .amount(Amount.of(100, java.util.Currency.getInstance("PHP")))
                .description("old")
                .transactionDate(LocalDate.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PatchTransactionCommand command = PatchTransactionCommand.builder()
                .id(transactionId)
                .accountId(newAccountId)
                .categoryId(newCategoryId)
                .description("updated")
                .userId(userId)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existing));
        when(accountRepository.findById(newAccountId)).thenReturn(Optional.of(Account.builder().id(newAccountId).userId(userId).name("NEW").currency(java.util.Currency.getInstance("PHP")).build()));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(Category.builder().id(newCategoryId).userId(userId).name("NEWCAT").build()));
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
}