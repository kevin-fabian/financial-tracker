package com.fabiankevin.app.services;

import com.fabiankevin.app.accounts.domain.models.Account;
import com.fabiankevin.app.accounts.domain.models.AccountSummary;
import com.fabiankevin.app.accounts.domain.models.AccountType;
import com.fabiankevin.app.accounts.domain.services.AccountService;
import com.fabiankevin.app.accounts.domain.services.SyncChangeAwareAccountService;
import com.fabiankevin.app.accounts.domain.services.commands.CreateAccountCommand;
import com.fabiankevin.app.accounts.domain.services.commands.PatchAccountCommand;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.services.SyncChangeService;
import com.fabiankevin.app.sync_devices.domain.services.commands.CreateSyncChangeCommand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static com.fabiankevin.app.accounts.domain.models.AccountType.E_WALLET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncChangeAwareAccountServiceTest {

    private final AccountService delegate = mock(AccountService.class);
    private final SyncChangeService syncChangeService = mock(SyncChangeService.class);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final SyncChangeAwareAccountService service = new SyncChangeAwareAccountService(
            delegate, syncChangeService, jsonMapper
    );

    @Nested
    class CreateAccount {

        @Test
        void givenNewAccount_shouldDelegateAndRegisterCreateChange() {
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            CreateAccountCommand command = CreateAccountCommand.builder()
                    .id(accountId)
                    .name("BDO")
                    .currency(Currency.getInstance("USD"))
                    .type(E_WALLET)
                    .userId(userId)
                    .build();

            AccountSummary expected = AccountSummary.builder()
                    .account(Account.builder()
                            .id(accountId)
                            .name("BDO")
                            .currency(Currency.getInstance("USD"))
                            .type(E_WALLET)
                            .active(true)
                            .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                            .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                            .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                            .build())
                    .totalBalance(1500.50)
                    .totalTransactions(42)
                    .build();

            when(delegate.createAccount(command)).thenReturn(expected);

            AccountSummary result = service.createAccount(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.ACCOUNTS);
            assertThat(change.entityId()).isEqualTo(accountId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.CREATE);
            Account capturedAccount = jsonMapper.readValue(change.payload(), Account.class);
            assertThat(capturedAccount).isEqualTo(expected.account());
            verify(delegate, never()).getAccountById(any(), any());
        }

        @Test
        void givenAccountWithZeroBalance_shouldRegisterChangeWithCorrectPayload() {
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            CreateAccountCommand command = CreateAccountCommand.builder()
                    .id(accountId)
                    .name("GCASH")
                    .currency(Currency.getInstance("PHP"))
                    .type(E_WALLET)
                    .userId(userId)
                    .build();

            AccountSummary expected = AccountSummary.builder()
                    .account(Account.builder()
                            .id(accountId)
                            .name("GCASH")
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .active(true)
                            .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                            .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                            .user(User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                            .build())
                    .totalBalance(0.0)
                    .totalTransactions(0)
                    .build();

            when(delegate.createAccount(command)).thenReturn(expected);

            AccountSummary result = service.createAccount(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            Account capturedAccount = jsonMapper.readValue(captor.getValue().payload(), Account.class);
            assertThat(capturedAccount).isEqualTo(expected.account());
        }
    }

    @Nested
    class PatchAccount {

        @Test
        void givenExistingAccount_shouldDelegateAndRegisterUpdateChange() {
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            PatchAccountCommand command = PatchAccountCommand.builder()
                    .id(accountId)
                    .name("GCASH_MAIN")
                    .currency(Currency.getInstance("PHP"))
                    .type(E_WALLET)
                    .userId(userId)
                    .build();

            AccountSummary expected = AccountSummary.builder()
                    .account(Account.builder()
                            .id(accountId)
                            .name("GCASH_MAIN")
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .active(true)
                            .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                            .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                            .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                            .build())
                    .totalBalance(500.0)
                    .totalTransactions(10)
                    .build();

            when(delegate.patchAccount(command)).thenReturn(expected);

            AccountSummary result = service.patchAccount(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            Account capturedAccount = jsonMapper.readValue(captor.getValue().payload(), Account.class);
            assertThat(capturedAccount).isEqualTo(expected.account());
        }

        @Test
        void givenAccountWithUpdatedFields_shouldRegisterChangeWithUpdatedPayload() {
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            PatchAccountCommand command = PatchAccountCommand.builder()
                    .id(accountId)
                    .name("PAYMAY_PREMIUM")
                    .currency(Currency.getInstance("EUR"))
                    .type(AccountType.BANK_ACCOUNT)
                    .userId(userId)
                    .build();

            AccountSummary expected = AccountSummary.builder()
                    .account(Account.builder()
                            .id(accountId)
                            .name("PAYMAY_PREMIUM")
                            .currency(Currency.getInstance("EUR"))
                            .type(AccountType.BANK_ACCOUNT)
                            .active(true)
                            .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                            .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                            .user(User.builder().id(userId).firstName("Alice").lastName("Wonder").build())
                            .build())
                    .totalBalance(9876.54)
                    .totalTransactions(128)
                    .build();

            when(delegate.patchAccount(command)).thenReturn(expected);

            AccountSummary result = service.patchAccount(command);

            assertThat(result).isSameAs(expected);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            Account capturedAccount = jsonMapper.readValue(captor.getValue().payload(), Account.class);
            assertThat(capturedAccount).isEqualTo(expected.account());
        }
    }

    @Nested
    class DisableAccount {

        @Test
        void givenActiveAccount_shouldDelegateAndRegisterDeleteChange() {
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();

            AccountSummary existing = AccountSummary.builder()
                    .account(Account.builder()
                            .id(accountId)
                            .name("GCASH")
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .active(true)
                            .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                            .updatedAt(Instant.parse("2025-06-01T10:00:00Z"))
                            .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                            .build())
                    .totalBalance(500.0)
                    .totalTransactions(10)
                    .build();

            when(delegate.getAccountById(accountId, userId)).thenReturn(existing);

            service.disableAccount(accountId, userId);

            verify(delegate, times(1)).getAccountById(accountId, userId);
            verify(delegate, times(1)).disableAccount(accountId, userId);
            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            CreateSyncChangeCommand change = captor.getValue();
            assertThat(change.userId()).isEqualTo(userId);
            assertThat(change.entityType()).isEqualTo(ResourceType.ACCOUNTS);
            assertThat(change.entityId()).isEqualTo(accountId);
            assertThat(change.operation()).isEqualTo(SyncChangeOperationType.DELETE);
            AccountSummary capturedSummary = jsonMapper.readValue(change.payload(), AccountSummary.class);
            assertThat(capturedSummary).isEqualTo(existing);
        }

        @Test
        void givenAccountWithTransactions_shouldRegisterDeleteChangeWithAccountState() {
            UUID userId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();

            AccountSummary existing = AccountSummary.builder()
                    .account(Account.builder()
                            .id(accountId)
                            .name("BDO")
                            .currency(Currency.getInstance("PHP"))
                            .type(AccountType.BANK_ACCOUNT)
                            .active(true)
                            .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                            .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                            .user(User.builder().id(userId).firstName("Jane").lastName("Smith").build())
                            .build())
                    .totalBalance(12345.67)
                    .totalTransactions(250)
                    .build();

            when(delegate.getAccountById(accountId, userId)).thenReturn(existing);

            service.disableAccount(accountId, userId);

            ArgumentCaptor<CreateSyncChangeCommand> captor = ArgumentCaptor.forClass(CreateSyncChangeCommand.class);
            verify(syncChangeService, times(1)).create(captor.capture());
            AccountSummary capturedSummary = jsonMapper.readValue(captor.getValue().payload(), AccountSummary.class);
            assertThat(capturedSummary).isEqualTo(existing);
        }
    }

    @Nested
    class NonMutatingMethods {

        @Test
        void getAccountById_shouldDelegateWithoutRegisteringChange() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            AccountSummary expected = AccountSummary.builder()
                    .account(Account.builder()
                            .id(id)
                            .name("GCASH")
                            .currency(Currency.getInstance("PHP"))
                            .type(E_WALLET)
                            .active(true)
                            .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                            .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                            .user(User.builder().id(userId).firstName("John").lastName("Doe").build())
                            .build())
                    .totalBalance(100.0)
                    .totalTransactions(5)
                    .build();

            when(delegate.getAccountById(id, userId)).thenReturn(expected);

            AccountSummary result = service.getAccountById(id, userId);

            assertThat(result).isSameAs(expected);
            verify(delegate, times(1)).getAccountById(id, userId);
            verify(syncChangeService, never()).create(any());
        }

        @Test
        void deleteAccountById_shouldDelegateWithoutRegisteringChange() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            service.deleteAccountById(id, userId);

            verify(delegate, times(1)).deleteAccountById(id, userId);
            verify(syncChangeService, never()).create(any());
        }
    }
}
