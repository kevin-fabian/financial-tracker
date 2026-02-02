package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Currency;
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

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(id, UUID.randomUUID()));
        verify(accountRepository, times(1)).findById(id);
    }

    @Test
    void getAccountById_givenNonExisting_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(id, UUID.randomUUID()));
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

        assertThrows(AccountNotFoundException.class, () -> accountService.deleteAccountById(id, UUID.randomUUID()));
        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, never()).deleteById(any());
    }
}
