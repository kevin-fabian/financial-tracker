package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.PatchAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultAccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private DefaultAccountService accountService;

    @Test
    void createAccount_givenValidCommand_thenShouldSaveAccount() {
        UUID userId = UUID.randomUUID();
        CreateAccountCommand command = CreateAccountCommand.builder()
                .name("GCASH")
                .currency(Currency.getInstance("PHP"))
                .userId(userId)
                .build();

        when(accountRepository.save(any())).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            return a.toBuilder().id(UUID.randomUUID()).build();
        });

        Account created = accountService.createAccount(command);

        assertEquals("GCASH", created.name());
        assertEquals(userId, created.userId());
        verify(accountRepository, times(1)).save(any());
    }

    @Test
    void getAccountById_givenExistingAndMatchingUser_thenShouldReturn() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(Account.builder()
                .id(id)
                .name("GCASH")
                .userId(userId)
                .currency(Currency.getInstance("PHP"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()));

        Account found = accountService.getAccountById(id, userId);

        assertEquals(id, found.id());
        assertEquals(userId, found.userId());
        verify(accountRepository, times(1)).findById(id);
    }

    @Test
    void getAccountById_givenExistingButDifferentUser_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(Account.builder()
                .id(id)
                .name("GCASH")
                .userId(UUID.randomUUID())
                .currency(Currency.getInstance("PHP"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()));

        UUID otherUser = UUID.randomUUID();
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(id, otherUser));
        verify(accountRepository, times(1)).findById(id);
    }

    @Test
    void getAccountById_givenNonExisting_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        UUID otherUser = UUID.randomUUID();
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(id, otherUser));
        verify(accountRepository, times(1)).findById(id);
    }

    @Test
    void deleteAccountById_givenExistingAndMatchingUser_thenShouldDelete() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(Account.builder()
                .id(id)
                .name("GCASH")
                .userId(userId)
                .currency(Currency.getInstance("PHP"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()));

        accountService.deleteAccountById(id, userId);

        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteAccountById_givenNonExisting_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        UUID otherUser = UUID.randomUUID();
        assertThrows(AccountNotFoundException.class, () -> accountService.deleteAccountById(id, otherUser));
        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, never()).deleteById(any());
    }

    @Test
    void getAccountsByPageAndUserId_givenUserId_thenShouldReturnPagedAccounts() {
        UUID userId = UUID.randomUUID();
        var accounts = List.of(
                Account.builder().id(UUID.randomUUID()).name("A1").userId(userId).currency(Currency.getInstance("PHP")).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                Account.builder().id(UUID.randomUUID()).name("A2").userId(userId).currency(Currency.getInstance("PHP")).createdAt(Instant.now()).updatedAt(Instant.now()).build()
        );

        Page<Account> page = new Page<>(accounts, 0, 10, accounts.size(), 1, true, true);

        PageQuery query = new PageQuery(0, 10, "name", "ASC");

        when(accountRepository.getAccountsByPageAndUserId(query, userId)).thenReturn(page);

        Page<Account> result = accountService.getAccountsByPageAndUserId(query, userId);

        assertEquals(page, result);
        verify(accountRepository, times(1)).getAccountsByPageAndUserId(query, userId);
    }

    @Test
    void patchAccount_givenValidCommand_thenShouldUpdateFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Account existing = Account.builder()
                .id(id)
                .name("GCASH")
                .userId(userId)
                .currency(Currency.getInstance("PHP"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PatchAccountCommand command = PatchAccountCommand.builder()
                .id(id)
                .name("GCASH_MAIN")
                .currency(Currency.getInstance("PHP"))
                .userId(userId)
                .build();

        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated = accountService.patchAccount(command);

        assertEquals("GCASH_MAIN", updated.name());
        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, times(1)).save(any());
    }

    @Test
    void patchAccount_givenNonExistingId_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PatchAccountCommand command = PatchAccountCommand.builder()
                .id(id)
                .name("GCASH_MAIN")
                .currency(Currency.getInstance("PHP"))
                .userId(userId)
                .build();

        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.patchAccount(command));
        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, never()).save(any());
    }
}
