package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.PatchAccountCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.AccountType.E_WALLET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAccountServiceTest {
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final UserClient userClient = mock(UserClient.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final HouseholdService householdService = mock(HouseholdService.class);
    private final AccountService accountService = new DefaultAccountService(accountRepository, transactionRepository, userClient, householdService);

    @Nested
    class CreateAccount {

        @Test
        void givenNewAccount_thenShouldReturnFullSummary() {
            UUID userId = UUID.randomUUID();
            UUID generatedId = UUID.randomUUID();
            CreateAccountCommand command = CreateAccountCommand.builder()
                    .name("BDO")
                    .currency(Currency.getInstance("USD"))
                    .type(E_WALLET)
                    .userId(userId)
                    .build();

            when(accountRepository.findByNameAndTypeAndUserId(command.name(), command.type(), command.userId()))
                    .thenReturn(Optional.empty());
            when(accountRepository.save(any())).thenAnswer(invocation -> {
                Account a = invocation.getArgument(0);
                return a.toBuilder().id(generatedId).build();
            });
            when(accountRepository.findSummaryByIdAndUserId(generatedId, userId))
                    .thenReturn(Optional.of(AccountSummary.builder()
                            .account(Account.builder()
                                    .id(generatedId)
                                    .name("BDO")
                                    .currency(Currency.getInstance("USD"))
                                    .type(E_WALLET)
                                    .active(true)
                                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                                    .user(User.builder()
                                            .id(userId)
                                            .firstName("John")
                                            .lastName("Doe")
                                            .build())
                                    .build())
                            .totalBalance(1500.50)
                            .totalTransactions(42)
                            .build()));

            AccountSummary created = accountService.createAccount(command);

            assertEquals("BDO", created.account().name());
            assertEquals(Currency.getInstance("USD"), created.account().currency());
            assertEquals(E_WALLET, created.account().type());
            assertTrue(created.account().active());
            assertEquals(Instant.parse("2025-01-01T00:00:00Z"), created.account().createdAt());
            assertEquals(Instant.parse("2025-01-01T00:00:00Z"), created.account().updatedAt());
            assertEquals(userId, created.account().user().id());
            assertEquals("John", created.account().user().firstName());
            assertEquals("Doe", created.account().user().lastName());
            assertEquals(1500.50, created.totalBalance());
            assertEquals(42, created.totalTransactions());
            verify(accountRepository, times(1)).save(any());
        }

        @Test
        void givenInactiveAccountWithSameNameAndType_thenShouldReactivate() {
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
            when(accountRepository.findSummaryByIdAndUserId(existingId, userId))
                    .thenReturn(Optional.of(AccountSummary.builder()
                            .account(Account.builder()
                                    .id(existingId)
                                    .name("GCASH")
                                    .user(User.of(userId))
                                    .currency(Currency.getInstance("PHP"))
                                    .type(E_WALLET)
                                    .active(true)
                                    .build())
                            .totalBalance(0.0)
                            .totalTransactions(0)
                            .build()));

            AccountSummary created = accountService.createAccount(command);

            assertEquals(existingId, created.account().id(), "should reuse existing account id");
            assertTrue(created.account().active(), "account should be reactivated");
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertTrue(captor.getValue().active(), "account should be reactivated");
        }

        @Test
        void givenActiveAccountWithSameNameAndType_thenShouldThrow() {
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
                    () -> accountService.createAccount(command),
                    "should throw when active duplicate exists");
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    class GetAccountById {

        @Test
        void givenExistingAndMatchingUser_thenShouldReturnSummary() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(accountRepository.findSummaryByIdAndUserId(id, userId))
                    .thenReturn(Optional.of(AccountSummary.builder()
                            .account(Account.builder()
                                    .id(id)
                                    .name("GCASH")
                                    .user(User.of(userId))
                                    .currency(Currency.getInstance("PHP"))
                                    .type(E_WALLET)
                                    .active(true)
                                    .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                                    .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                                    .build())
                            .totalBalance(1500.50)
                            .totalTransactions(42)
                            .build()));
            when(userClient.getUsersByIds(List.of(userId)))
                    .thenReturn(List.of(User.builder()
                            .id(userId)
                            .firstName("John")
                            .lastName("Doe")
                            .build()));

            AccountSummary found = accountService.getAccountById(id, userId);

            assertEquals(id, found.account().id(), "id should match");
            assertEquals(userId, found.account().user().id(), "user id should match");
            assertEquals("GCASH", found.account().name(), "name should match");
            assertEquals(1500.50, found.totalBalance(), "totalBalance should match");
            assertEquals(42, found.totalTransactions(), "totalTransactions should match");
            assertEquals("John", found.account().user().firstName(), "enriched firstName should match");
            assertEquals("Doe", found.account().user().lastName(), "enriched lastName should match");
            verify(accountRepository, times(1)).findSummaryByIdAndUserId(id, userId);
        }

        @Test
        void givenExistingButDifferentUser_thenShouldThrow() {
            UUID id = UUID.randomUUID();
            when(accountRepository.findSummaryByIdAndUserId(id, UUID.randomUUID()))
                    .thenReturn(Optional.empty());

            UUID otherUser = UUID.randomUUID();
            assertThrows(AccountNotFoundException.class,
                    () -> accountService.getAccountById(id, otherUser),
                    "should throw when user does not own account");
            verify(accountRepository, times(1)).findSummaryByIdAndUserId(id, otherUser);
        }

        @Test
        void givenNonExisting_thenShouldThrow() {
            UUID id = UUID.randomUUID();
            when(accountRepository.findSummaryByIdAndUserId(id, UUID.randomUUID()))
                    .thenReturn(Optional.empty());

            UUID otherUser = UUID.randomUUID();
            assertThrows(AccountNotFoundException.class,
                    () -> accountService.getAccountById(id, otherUser),
                    "should throw when account does not exist");
            verify(accountRepository, times(1)).findSummaryByIdAndUserId(id, otherUser);
        }
    }

    @Nested
    class DeleteAccountById {

        @Test
        void givenExistingAndMatchingUser_thenShouldDelete() {
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
        void givenNonExisting_thenShouldThrow() {
            UUID id = UUID.randomUUID();
            when(accountRepository.findById(id)).thenReturn(Optional.empty());

            UUID otherUser = UUID.randomUUID();
            assertThrows(AccountNotFoundException.class,
                    () -> accountService.deleteAccountById(id, otherUser),
                    "should throw when account does not exist");
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, never()).deleteById(any());
        }
    }

    @Nested
    class PatchAccount {

        @Test
        void givenExistingAccount_thenShouldUpdateFields() {
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
            when(accountRepository.findSummaryByIdAndUserId(id, userId))
                    .thenReturn(Optional.of(AccountSummary.builder()
                            .account(Account.builder()
                                    .id(id)
                                    .name("GCASH_MAIN")
                                    .user(User.of(userId))
                                    .currency(Currency.getInstance("PHP"))
                                    .type(E_WALLET)
                                    .active(true)
                                    .build())
                            .totalBalance(0.0)
                            .totalTransactions(0)
                            .build()));

            AccountSummary updated = accountService.patchAccount(command);

            assertEquals("GCASH_MAIN", updated.account().name(), "name should be updated");
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, times(1)).save(any());
        }

        @Test
        void givenExistingAccountWithUserDetails_thenShouldReturnFullSummary() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Account existing = Account.builder()
                    .id(id)
                    .name("GCASH")
                    .user(User.of(userId))
                    .currency(Currency.getInstance("PHP"))
                    .type(E_WALLET)
                    .active(true)
                    .createdAt(Instant.parse("2024-06-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2024-06-01T10:00:00Z"))
                    .build();

            PatchAccountCommand command = PatchAccountCommand.builder()
                    .id(id)
                    .name("GCASH_PREMIUM")
                    .currency(Currency.getInstance("EUR"))
                    .type(com.fabiankevin.app.models.enums.AccountType.BANK_ACCOUNT)
                    .userId(userId)
                    .build();

            when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
            when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(accountRepository.findSummaryByIdAndUserId(id, userId))
                    .thenReturn(Optional.of(AccountSummary.builder()
                            .account(Account.builder()
                                    .id(id)
                                    .name("GCASH_PREMIUM")
                                    .currency(Currency.getInstance("EUR"))
                                    .type(com.fabiankevin.app.models.enums.AccountType.BANK_ACCOUNT)
                                    .active(true)
                                    .createdAt(Instant.parse("2024-06-01T10:00:00Z"))
                                    .updatedAt(Instant.parse("2024-06-01T10:00:00Z"))
                                    .user(User.builder()
                                            .id(userId)
                                            .firstName("Jane")
                                            .lastName("Smith")
                                            .build())
                                    .build())
                            .totalBalance(9876.54)
                            .totalTransactions(128)
                            .build()));

            AccountSummary updated = accountService.patchAccount(command);

            assertEquals("GCASH_PREMIUM", updated.account().name());
            assertEquals(Currency.getInstance("EUR"), updated.account().currency());
            assertEquals(com.fabiankevin.app.models.enums.AccountType.BANK_ACCOUNT, updated.account().type());
            assertTrue(updated.account().active());
            assertEquals(Instant.parse("2024-06-01T10:00:00Z"), updated.account().createdAt());
            assertEquals(Instant.parse("2024-06-01T10:00:00Z"), updated.account().updatedAt());
            assertEquals(userId, updated.account().user().id());
            assertEquals("Jane", updated.account().user().firstName());
            assertEquals("Smith", updated.account().user().lastName());
            assertEquals(9876.54, updated.totalBalance());
            assertEquals(128, updated.totalTransactions());
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, times(1)).save(any());
        }

        @Test
        void givenNonExistingId_thenShouldThrow() {
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

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.patchAccount(command),
                    "should throw when account does not exist");
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    class DisableAccount {

        @Test
        void givenExistingAndMatchingUser_thenShouldDisableAccount() {
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

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertFalse(captor.getValue().active(), "account should be disabled");
        }

        @Test
        void givenNonExisting_thenShouldThrow() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(accountRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.disableAccount(id, userId),
                    "should throw when account does not exist");
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, never()).save(any());
        }

        @Test
        void givenDifferentUser_thenShouldThrow() {
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

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.disableAccount(id, userId),
                    "should throw when user does not own account");
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, never()).save(any());
        }

        @Test
        void givenAlreadyDisabled_thenShouldStillSucceedIdempotent() {
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

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            assertFalse(captor.getValue().active(), "account should remain disabled");
        }
    }

    @Nested
    class DeleteAllByUserId {

        @Test
        void givenAccounts_shouldDeleteAll() {
            UUID userId = UUID.randomUUID();

            accountService.deleteAllByUserId(userId);

            verify(accountRepository, times(1)).deleteAllByUserId(userId);
        }

        @Test
        void givenNoAccounts_shouldStillSucceed() {
            UUID userId = UUID.randomUUID();

            accountService.deleteAllByUserId(userId);

            verify(accountRepository, times(1)).deleteAllByUserId(userId);
        }

        @Test
        void isTransactional_shouldRollbackOnException() {
            UUID userId = UUID.randomUUID();
            when(accountRepository.deleteAllByUserId(userId)).thenThrow(new RuntimeException("database error"));

            assertThrows(RuntimeException.class,
                    () -> accountService.deleteAllByUserId(userId),
                    "should propagate transaction rollback");
            verify(accountRepository, times(1)).deleteAllByUserId(userId);
        }
    }
}
