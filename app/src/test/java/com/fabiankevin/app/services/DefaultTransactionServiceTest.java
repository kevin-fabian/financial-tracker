package com.fabiankevin.app.services;

import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Amount;
import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.CategoryRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        AddTransactionCommand command = AddTransactionCommand.builder()
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
                .build()));
        when(categoryRepository.findById(command.categoryId())).thenReturn(Optional.of(Category.builder()
                        .id(command.categoryId())
                        .name("FOOD")
                        .userId(UUID.randomUUID())
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
}