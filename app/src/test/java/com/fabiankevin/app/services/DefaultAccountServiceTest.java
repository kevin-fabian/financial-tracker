package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.PatchAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.AccountType.E_WALLET;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultAccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DefaultAccountService accountService;

    @Test
    void createAccount_givenNewAccount_thenShouldSaveAccount() {
        UUID userId = UUID.randomUUID();
        CreateAccountCommand command = CreateAccountCommand.builder()
                .name("GCASH")
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .userId(userId)
                .build();

        when(accountRepository.findByNameAndTypeAndUserId(command.name(), command.type(), command.userId()))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            return a.toBuilder().id(UUID.randomUUID()).build();
        });

        Account created = accountService.createAccount(command);

        assertEquals("GCASH", created.name());
        assertEquals(userId, created.user().id());
        assertTrue(created.active());
        verify(accountRepository, times(1)).save(any());
    }

    @Test
    void createAccount_givenInactiveAccountWithSameNameAndType_thenShouldReactivate() {
        UUID userId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        CreateAccountCommand command = CreateAccountCommand.builder()
                .name("GCASH")
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .userId(userId)
                .build();

        Account inactive = Account.builder()
                .id(existingId)
                .name("GCASH")
                .user(User.of(userId))
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(accountRepository.findByNameAndTypeAndUserId(command.name(), command.type(), command.userId()))
                .thenReturn(Optional.of(inactive));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = accountService.createAccount(command);

        assertEquals(existingId, created.id());
        assertTrue(created.active());
        verify(accountRepository, times(1)).save(any());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertTrue(captor.getValue().active(), "account should be reactivated");
    }

    @Test
    void createAccount_givenActiveAccountWithSameNameAndType_thenShouldThrow() {
        UUID userId = UUID.randomUUID();
        CreateAccountCommand command = CreateAccountCommand.builder()
                .name("GCASH")
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .userId(userId)
                .build();

        Account active = Account.builder()
                .id(UUID.randomUUID())
                .name("GCASH")
                .user(User.of(userId))
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(accountRepository.findByNameAndTypeAndUserId(command.name(), command.type(), command.userId()))
                .thenReturn(Optional.of(active));

        assertThrows(com.fabiankevin.app.exceptions.AccountAlreadyExistException.class,
                () -> accountService.createAccount(command));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getAccountById_givenExistingAndMatchingUser_thenShouldReturn() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(Account.builder()
                .id(id)
                .name("GCASH")
                .user(User.of(userId))
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()));

        Account found = accountService.getAccountById(id, userId);

        assertEquals(id, found.id());
        assertEquals(userId, found.user().id());
        verify(accountRepository, times(1)).findById(id);
    }

    @Test
    void getAccountById_givenExistingButDifferentUser_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(Account.builder()
                .id(id)
                .name("GCASH")
                .user(User.of(UUID.randomUUID()))
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
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
                .user(User.of(userId))
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
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
                Account.builder().id(UUID.randomUUID()).name("A1").user(User.of(userId)).currency(Currency.getInstance("PHP")).type(E_WALLET).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                Account.builder().id(UUID.randomUUID()).name("A2").user(User.of(userId)).currency(Currency.getInstance("PHP")).type(E_WALLET).createdAt(Instant.now()).updatedAt(Instant.now()).build()
        );

        Page<Account> page = new Page<>(accounts, 0, 10, accounts.size(), 1, true, true);

        PageQuery query = new PageQuery(0, 10, "name", "ASC");

        when(accountRepository.getAccountsByPageAndUserId(query, userId)).thenReturn(page);

        Page<Account> result = accountService.getAccountsByPageAndUserId(query, userId);

        assertEquals(page, result);
        verify(accountRepository, times(1)).getAccountsByPageAndUserId(query, userId);
    }

    @Test
    void patchAccount_givenExistingAccount_thenShouldUpdateFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Account existing = Account.builder()
                .id(id)
                .name("GCASH")
                .user(User.of(userId))
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PatchAccountCommand command = PatchAccountCommand.builder()
                .id(id)
                .name("GCASH_MAIN")
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
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
                .type(E_WALLET)
                .userId(userId)
                .build();

        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.patchAccount(command));
        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void disableAccount_givenExistingAndMatchingUser_thenShouldDisableAccount() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Account existing = Account.builder()
                .id(id)
                .name("GCASH")
                .user(User.of(userId))
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(transactionRepository.countByAccountId(existing.id())).thenReturn(2L);
        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.disableAccount(id, userId);

        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, times(1)).save(any());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertFalse(captor.getValue().active(), "account should be disabled");
    }

    @Test
    void disableAccount_givenNonExisting_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.disableAccount(id, userId));
        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void disableAccount_givenDifferentUser_thenShouldThrow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        Account existing = Account.builder()
                .id(id)
                .name("GCASH")
                .user(User.of(otherUserId))
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(AccountNotFoundException.class, () -> accountService.disableAccount(id, userId));
        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void disableAccount_givenAlreadyDisabled_thenShouldStillSucceedIdempotent() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Account existing = Account.builder()
                .id(id)
                .name("GCASH")
                .user(User.of(userId))
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .active(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(transactionRepository.countByAccountId(existing.id())).thenReturn(2L);
        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.disableAccount(id, userId);

        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, times(1)).save(any());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertFalse(captor.getValue().active(), "account should remain disabled");
    }

    @Test
    void deleteAllByUserId_givenAccounts_shouldDeleteAll() {
        UUID userId = UUID.randomUUID();

        accountService.deleteAllByUserId(userId);

        verify(accountRepository, times(1)).deleteAllByUserId(userId);
    }

    @Test
    void deleteAllByUserId_givenNoAccounts_shouldStillSucceed() {
        UUID userId = UUID.randomUUID();

        accountService.deleteAllByUserId(userId);

        verify(accountRepository, times(1)).deleteAllByUserId(userId);
    }

    @Test
    void deleteAllByUserId_isTransactional_shouldRollbackOnException() {
        UUID userId = UUID.randomUUID();
        when(accountRepository.deleteAllByUserId(userId)).thenThrow(new RuntimeException("database error"));

        assertThrows(RuntimeException.class, () -> accountService.deleteAllByUserId(userId));
        verify(accountRepository, times(1)).deleteAllByUserId(userId);
    }
}
