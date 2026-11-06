package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.AccountSummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.AccountType;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.commands.PatchAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.AccountType.E_WALLET;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
        void givenNewAccount_shouldReturnFullSummary() {
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

            assertEquals(generatedId, created.account().id(), "id should match");
            assertEquals("BDO", created.account().name(), "name should match");
            assertEquals(Currency.getInstance("USD"), created.account().currency(), "currency should match");
            assertEquals(E_WALLET, created.account().type(), "type should match");
            assertTrue(created.account().active(), "account should be active");
            assertEquals(Instant.parse("2025-01-01T00:00:00Z"), created.account().createdAt(), "createdAt should match");
            assertEquals(Instant.parse("2025-01-01T00:00:00Z"), created.account().updatedAt(), "updatedAt should match");
            assertEquals(userId, created.account().user().id(), "user id should match");
            assertEquals("John", created.account().user().firstName(), "enriched firstName should match");
            assertEquals("Doe", created.account().user().lastName(), "enriched lastName should match");
            assertEquals(1500.50, created.totalBalance(), "totalBalance should match");
            assertEquals(42, created.totalTransactions(), "totalTransactions should match");
            verify(accountRepository, times(1)).save(any());
        }

        @Test
        void givenInactiveAccountWithSameNameAndType_shouldReactivate() {
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
            when(accountRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
            verify(accountRepository).update(captor.capture());
            assertTrue(captor.getValue().active(), "account should be reactivated");
        }

        @Test
        void givenActiveAccountWithSameNameAndType_shouldThrow() {
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

            assertThatThrownBy(() -> accountService.createAccount(command))
                    .isInstanceOf(com.fabiankevin.app.exceptions.AccountAlreadyExistException.class)
                    .hasMessageContaining("Account with the same name and type already exists");
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    class GetAccountById {

        @Test
        void givenExistingAndMatchingUser_shouldReturnSummary() {
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
        void givenExistingButDifferentUser_shouldThrow() {
            UUID id = UUID.randomUUID();
            UUID otherUser = UUID.randomUUID();
            when(accountRepository.findSummaryByIdAndUserId(id, otherUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getAccountById(id, otherUser))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(accountRepository, times(1)).findSummaryByIdAndUserId(id, otherUser);
        }

        @Test
        void givenNonExisting_shouldThrow() {
            UUID id = UUID.randomUUID();
            UUID otherUser = UUID.randomUUID();
            when(accountRepository.findSummaryByIdAndUserId(id, otherUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getAccountById(id, otherUser))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(accountRepository, times(1)).findSummaryByIdAndUserId(id, otherUser);
        }
    }

    @Nested
    class DeleteAccount {

        @Test
        void givenExistingAndMatchingUser_shouldDelete() {
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
        void givenNonExisting_shouldThrow() {
            UUID id = UUID.randomUUID();
            UUID otherUser = UUID.randomUUID();
            when(accountRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.deleteAccountById(id, otherUser))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, never()).deleteById(any());
        }
    }

    @Nested
    class PatchAccount {

        @Test
        void givenExistingAccount_shouldUpdateFields() {
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
            when(accountRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
            verify(accountRepository, times(1)).update(any());
        }

        @Test
        void givenExistingAccountWithUserDetails_shouldReturnFullSummary() {
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
                    .type(AccountType.BANK_ACCOUNT)
                    .userId(userId)
                    .build();

            when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
            when(accountRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(accountRepository.findSummaryByIdAndUserId(id, userId))
                    .thenReturn(Optional.of(AccountSummary.builder()
                            .account(Account.builder()
                                    .id(id)
                                    .name("GCASH_PREMIUM")
                                    .currency(Currency.getInstance("EUR"))
                                    .type(AccountType.BANK_ACCOUNT)
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

            assertEquals("GCASH_PREMIUM", updated.account().name(), "name should be updated");
            assertEquals(Currency.getInstance("EUR"), updated.account().currency(), "currency should be updated");
            assertEquals(AccountType.BANK_ACCOUNT, updated.account().type(), "type should be updated");
            assertTrue(updated.account().active(), "account should be active");
            assertEquals(Instant.parse("2024-06-01T10:00:00Z"), updated.account().createdAt(), "createdAt should be preserved");
            assertEquals(Instant.parse("2024-06-01T10:00:00Z"), updated.account().updatedAt(), "updatedAt should be preserved");
            assertEquals(userId, updated.account().user().id(), "user id should match");
            assertEquals("Jane", updated.account().user().firstName(), "enriched firstName should match");
            assertEquals("Smith", updated.account().user().lastName(), "enriched lastName should match");
            assertEquals(9876.54, updated.totalBalance(), "totalBalance should match");
            assertEquals(128, updated.totalTransactions(), "totalTransactions should match");
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, times(1)).update(any());
        }

        @Test
        void givenNonExistingId_shouldThrow() {
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

            assertThatThrownBy(() -> accountService.patchAccount(command))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    class DisableAccount {

        @Test
        void givenExistingAndMatchingUser_shouldDisableAccount() {
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
            when(accountRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            accountService.disableAccount(id, userId);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).update(captor.capture());
            assertFalse(captor.getValue().active(), "account should be disabled");
        }

        @Test
        void givenNonExisting_shouldThrow() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(accountRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.disableAccount(id, userId))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, never()).update(any());
        }

        @Test
        void givenDifferentUser_shouldThrow() {
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

            assertThatThrownBy(() -> accountService.disableAccount(id, userId))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(accountRepository, times(1)).findById(id);
            verify(accountRepository, never()).update(any());
        }

        @Test
        void givenAlreadyDisabled_shouldStillSucceedIdempotent() {
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
            when(accountRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            accountService.disableAccount(id, userId);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).update(captor.capture());
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
        void givenDatabaseError_shouldRollback() {
            UUID userId = UUID.randomUUID();
            when(accountRepository.deleteAllByUserId(userId)).thenThrow(new RuntimeException("database error"));

            assertThatThrownBy(() -> accountService.deleteAllByUserId(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("database error");
            verify(accountRepository, times(1)).deleteAllByUserId(userId);
        }
    }

    @Nested
    class CreateBatchAccounts {

        @Test
        @SuppressWarnings("unchecked")
        void givenMultipleNewAccounts_shouldCreateAllWithSaveAll() {
            UUID userId = UUID.randomUUID();
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            CreateAccountCommand cmd1 = CreateAccountCommand.builder()
                    .name("GCASH")
                    .currency(Currency.getInstance("PHP"))
                    .type(E_WALLET)
                    .userId(userId)
                    .build();

            CreateAccountCommand cmd2 = CreateAccountCommand.builder()
                    .name("BDO")
                    .currency(Currency.getInstance("USD"))
                    .type(AccountType.BANK_ACCOUNT)
                    .userId(userId)
                    .build();

            when(accountRepository.findByNameAndTypeAndUserIdBatch(List.of(cmd1, cmd2), userId))
                    .thenReturn(List.of());
            ArgumentCaptor<List<Account>> saveCaptor = ArgumentCaptor.forClass(List.class);
            when(accountRepository.saveAll(saveCaptor.capture())).thenAnswer(invocation -> {
                List<Account> accounts = invocation.getArgument(0);
                return accounts.stream().map(account -> {
                    if ("GCASH".equals(account.name())) return account.toBuilder().id(id1).build();
                    if ("BDO".equals(account.name())) return account.toBuilder().id(id2).build();
                    return account;
                }).toList();
            });

            accountService.createBatchAccounts(List.of(cmd1, cmd2), userId);

            verify(accountRepository, times(1)).findByNameAndTypeAndUserIdBatch(List.of(cmd1, cmd2), userId);
            List<Account> saved = saveCaptor.getValue();
            assertEquals(2, saved.size(), "should save exactly 2 accounts");
        }

        @Test
        @SuppressWarnings("unchecked")
        void givenBatchWithReactivatedAccount_shouldNotSaveWithSaveAll() {
            UUID userId = UUID.randomUUID();
            UUID existingId = UUID.randomUUID();

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

            CreateAccountCommand cmd = CreateAccountCommand.builder()
                    .name("GCASH")
                    .currency(Currency.getInstance("PHP"))
                    .type(E_WALLET)
                    .userId(userId)
                    .id(existingId)
                    .build();

            when(accountRepository.findByNameAndTypeAndUserIdBatch(List.of(cmd), userId))
                    .thenReturn(List.of(inactive));
            ArgumentCaptor<List<Account>> saveCaptor = ArgumentCaptor.forClass(List.class);
            when(accountRepository.saveAll(saveCaptor.capture())).thenAnswer(invocation ->
                    invocation.getArgument(0));

            accountService.createBatchAccounts(List.of(cmd), userId);

            verify(accountRepository, times(1)).findByNameAndTypeAndUserIdBatch(List.of(cmd), userId);
            List<Account> saved = saveCaptor.getValue();
            assertEquals(0, saved.size(), "should not save any accounts when all are reactivated");
        }

        @Test
        @SuppressWarnings("unchecked")
        void givenBatchWithNewAndReactivated_shouldSaveOnlyNewWithSaveAll() {
            UUID userId = UUID.randomUUID();
            UUID existingId = UUID.randomUUID();
            UUID newId = UUID.randomUUID();

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

            CreateAccountCommand cmd1 = CreateAccountCommand.builder()
                    .name("GCASH")
                    .currency(Currency.getInstance("PHP"))
                    .type(E_WALLET)
                    .userId(userId)
                    .id(existingId)
                    .build();

            CreateAccountCommand cmd2 = CreateAccountCommand.builder()
                    .name("BDO")
                    .currency(Currency.getInstance("USD"))
                    .type(AccountType.BANK_ACCOUNT)
                    .userId(userId)
                    .build();

            when(accountRepository.findByNameAndTypeAndUserIdBatch(List.of(cmd1, cmd2), userId))
                    .thenReturn(List.of(inactive));
            ArgumentCaptor<List<Account>> saveCaptor = ArgumentCaptor.forClass(List.class);
            when(accountRepository.saveAll(saveCaptor.capture())).thenAnswer(invocation -> {
                List<Account> accounts = invocation.getArgument(0);
                return accounts.stream().map(a -> {
                    if ("BDO".equals(a.name())) return a.toBuilder().id(newId).build();
                    return a;
                }).toList();
            });

            accountService.createBatchAccounts(List.of(cmd1, cmd2), userId);

            verify(accountRepository, times(1)).findByNameAndTypeAndUserIdBatch(List.of(cmd1, cmd2), userId);
            List<Account> saved = saveCaptor.getValue();
            assertEquals(1, saved.size(), "should save exactly 1 account (only the new one)");
            Account newAccount = saved.getFirst();
            assertEquals("BDO", newAccount.name(), "should only save the new account");
        }

        @Test
        void givenEmptyList_shouldNotCallRepository() {
            UUID userId = UUID.randomUUID();
            List<CreateAccountCommand> commands = List.of();

            accountService.createBatchAccounts(commands, userId);

            verify(accountRepository, never()).findByNameAndTypeAndUserIdBatch(anyList(), any());
            verify(accountRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    class GetAccountSummariesByPageQuery {

        @Test
        void givenHouseholdAccounts_shouldReturnEnrichedPage() {
            UUID userId = UUID.randomUUID();
            UUID householdMemberId = UUID.randomUUID();

            when(householdService.getHouseholdMembersUserIds(userId))
                    .thenReturn(List.of(userId, householdMemberId));

            AccountSummary summary1 = AccountSummary.builder()
                    .account(Account.builder()
                            .id(UUID.randomUUID())
                            .name("GCASH")
                            .user(User.of(userId))
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .active(true)
                            .build())
                    .totalBalance(5000.0)
                    .totalTransactions(10)
                    .build();

            AccountSummary summary2 = AccountSummary.builder()
                    .account(Account.builder()
                            .id(UUID.randomUUID())
                            .name("BDO")
                            .user(User.of(householdMemberId))
                            .currency(Currency.getInstance("USD"))
                            .type(AccountType.BANK_ACCOUNT)
                            .active(true)
                            .build())
                    .totalBalance(10000.0)
                    .totalTransactions(25)
                    .build();

            Page<AccountSummary> page = Page.<AccountSummary>builder()
                    .content(List.of(summary1, summary2))
                    .page(0)
                    .size(10)
                    .totalElements(2)
                    .totalPages(1)
                    .last(true)
                    .first(true)
                    .build();

            when(accountRepository.findAllByPageQueryWithSummary(any(), anyList(), any(), any()))
                    .thenReturn(page);
            when(userClient.getUsersByIds(List.of(userId, householdMemberId)))
                    .thenReturn(List.of(
                            User.builder().id(userId).firstName("John").lastName("Doe").build(),
                            User.builder().id(householdMemberId).firstName("Jane").lastName("Smith").build()
                    ));

            PageQuery query = PageQuery.withDefaults();
            LocalDate monthStart = LocalDate.of(2025, 10, 1);
            LocalDate monthEnd = LocalDate.of(2025, 10, 31);

            Page<AccountSummary> result = accountService.getAccountSummariesByPageQuery(query, userId, monthStart, monthEnd);

            assertEquals(2, result.content().size(), "should return 2 accounts");
            assertEquals(5000.0, result.content().getFirst().totalBalance(), "first account balance should match");
            assertEquals(10000.0, result.content().get(1).totalBalance(), "second account balance should match");
            assertEquals("John", result.content().getFirst().account().user().firstName(), "enriched firstName should match");
            assertEquals("Jane", result.content().get(1).account().user().firstName(), "enriched household member firstName should match");
            assertEquals(0, result.page(), "page should match");
            assertEquals(10, result.size(), "size should match");
            assertEquals(2, result.totalElements(), "totalElements should match");
            verify(accountRepository, times(1)).findAllByPageQueryWithSummary(eq(query), eq(List.of(userId, householdMemberId)), eq(monthStart), eq(monthEnd));
            verify(userClient, times(1)).getUsersByIds(List.of(userId, householdMemberId));
        }

        @Test
        void givenEmptyPage_shouldReturnEmptyPage() {
            UUID userId = UUID.randomUUID();

            when(householdService.getHouseholdMembersUserIds(userId))
                    .thenReturn(List.of(userId));

            Page<AccountSummary> emptyPage = Page.<AccountSummary>builder()
                    .content(List.of())
                    .page(0)
                    .size(10)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .first(true)
                    .build();

            when(accountRepository.findAllByPageQueryWithSummary(any(), anyList(), any(), any()))
                    .thenReturn(emptyPage);

            PageQuery query = PageQuery.withDefaults();
            LocalDate monthStart = LocalDate.of(2025, 10, 1);
            LocalDate monthEnd = LocalDate.of(2025, 10, 31);

            Page<AccountSummary> result = accountService.getAccountSummariesByPageQuery(query, userId, monthStart, monthEnd);

            assertTrue(result.content().isEmpty(), "content should be empty");
            assertEquals(0, result.totalElements(), "totalElements should be 0");
            verify(accountRepository, times(1)).findAllByPageQueryWithSummary(eq(query), eq(List.of(userId)), eq(monthStart), eq(monthEnd));
            verify(userClient, never()).getUsersByIds(anyList());
        }
    }
}
