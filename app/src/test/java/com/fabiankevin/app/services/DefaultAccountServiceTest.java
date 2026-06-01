package com.fabiankevin.app.services;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.IconData;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.persistence.AccountRepository;
import com.fabiankevin.app.persistence.IconRepository;
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
    private IconRepository iconRepository;

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
    void createAccount_givenExistingIcon_shouldResolveAndSaveAccount() {
        UUID userId = UUID.randomUUID();
        IconData existingIcon = IconData.builder()
                .id(UUID.randomUUID())
                .codePoint(0x1F697)
                .fontFamily("MaterialIcons")
                .iconName("car")
                .build();

        CreateAccountCommand command = CreateAccountCommand.builder()
                .name("CAR_ACCOUNT")
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .userId(userId)
                .icon(com.fabiankevin.app.models.IconData.builder()
                        .id(existingIcon.id())
                        .codePoint(0x1F697)
                        .fontFamily("MaterialIcons")
                        .iconName("car")
                        .build())
                .build();

        when(iconRepository.findByCodePointAndFontFamily(0x1F697, "MaterialIcons"))
                .thenReturn(Optional.of(existingIcon));
        when(accountRepository.save(any())).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            return a.toBuilder().id(UUID.randomUUID()).build();
        });

        Account created = accountService.createAccount(command);

        assertEquals("CAR_ACCOUNT", created.name());
        assertEquals(existingIcon, created.icon());
        verify(iconRepository, times(1)).findByCodePointAndFontFamily(0x1F697, "MaterialIcons");
        verify(accountRepository, times(1)).save(any());
    }

    @Test
    void createAccount_givenNewIcon_shouldBuildAndSaveAccount() {
        UUID userId = UUID.randomUUID();
        IconData newIcon = IconData.builder()
                .codePoint(0x1F354)
                .fontFamily("MaterialIcons")
                .iconName("restaurant")
                .build();

        CreateAccountCommand command = CreateAccountCommand.builder()
                .name("FOOD_ACCOUNT")
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .userId(userId)
                .icon(newIcon)
                .build();

        when(iconRepository.findByCodePointAndFontFamily(anyInt(), any())).thenReturn(Optional.empty());
        when(iconRepository.save(any())).thenAnswer(invocation -> (IconData) invocation.getArguments()[0]);

        when(accountRepository.save(any())).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            return a.toBuilder().id(UUID.randomUUID()).build();
        });

        Account created = accountService.createAccount(command);

        assertEquals("FOOD_ACCOUNT", created.name());
        assertEquals(newIcon.codePoint(), created.icon().codePoint());
        assertEquals(newIcon.fontFamily(), created.icon().fontFamily());
        assertEquals(newIcon.iconName(), created.icon().iconName());
        assertNull(created.icon().id());
        verify(iconRepository, times(1)).findByCodePointAndFontFamily(0x1F354, "MaterialIcons");
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(1)).save(accountArgumentCaptor.capture());
        Account value = accountArgumentCaptor.getValue();
        assertNull(value.id(), "id");
        assertEquals(value.name(), created.name(), "name");
        assertEquals(value.currency(), created.currency(), "currency");
        assertEquals(value.type(), created.type(), "type");
        assertEquals(value.userId(), created.userId(), "userId");
        assertNull(value.icon().id(), "icon.id");
        assertEquals(newIcon.codePoint(), value.icon().codePoint(), "codePoint");
        assertEquals(newIcon.fontFamily(), value.icon().fontFamily(), "fontFamily");
        assertNotNull(value.icon().createdAt(), "createdAt");
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
                .type(E_WALLET)
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
                .userId(userId)
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
                Account.builder().id(UUID.randomUUID()).name("A1").userId(userId).currency(Currency.getInstance("PHP")).type(E_WALLET).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                Account.builder().id(UUID.randomUUID()).name("A2").userId(userId).currency(Currency.getInstance("PHP")).type(E_WALLET).createdAt(Instant.now()).updatedAt(Instant.now()).build()
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
                .userId(userId)
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
    void patchAccount_givenNewIcon_shouldUpdateAccountIcon() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(iconRepository.findByCodePointAndFontFamily(anyInt(), any())).thenReturn(Optional.empty());
        when(iconRepository.save(any())).thenAnswer(invocation -> (IconData) invocation.getArguments()[0]);

        Account existing = Account.builder()
                .id(id)
                .name("GCASH")
                .userId(userId)
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        IconData newIcon = IconData.builder()
                .codePoint(0x1F697)
                .fontFamily("MaterialIcons")
                .iconName("car")
                .build();

        PatchAccountCommand command = PatchAccountCommand.builder()
                .id(id)
                .name("GCASH_MAIN")
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .userId(userId)
                .icon(newIcon)
                .build();

        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated = accountService.patchAccount(command);

        assertEquals("GCASH_MAIN", updated.name());
        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, times(1)).save(any());
        verify(iconRepository, times(1)).save(any());
        verify(iconRepository, times(1)).findByCodePointAndFontFamily(newIcon.codePoint(), "MaterialIcons");
    }

    @Test
    void patchAccount_givenExistingIcon_shouldUpdateAccountIcon() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        IconData existingIcon = IconData.builder()
                .id(UUID.randomUUID())
                .codePoint(0x1F697)
                .fontFamily("MaterialIcons")
                .iconName("car")
                .build();

        Account existing = Account.builder()
                .id(id)
                .name("GCASH")
                .userId(userId)
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .icon(existingIcon)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        IconData newIcon = IconData.builder()
                .codePoint(0x1F354)
                .fontFamily("MaterialIcons")
                .iconName("restaurant")
                .build();

        PatchAccountCommand command = PatchAccountCommand.builder()
                .id(id)
                .name("GCASH_MAIN")
                .currency(Currency.getInstance("PHP"))
                .type(E_WALLET)
                .userId(userId)
                .icon(newIcon)
                .build();

        when(iconRepository.findByCodePointAndFontFamily(anyInt(), any())).thenReturn(Optional.of(newIcon.toBuilder().id(UUID.randomUUID()).build()));

        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account updated = accountService.patchAccount(command);

        IconData updatedIcon = updated.icon();

        assertEquals("GCASH_MAIN", updated.name());
        assertNotNull(updatedIcon, "icon");
        assertNotNull(updatedIcon.id(), "icon.id");
        assertEquals(updatedIcon.fontFamily(), "MaterialIcons", "icon.fontFamily");
        assertEquals(updatedIcon.codePoint(), 0x1F354, "icon.codePoint");
        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, times(1)).save(any());
    }
}
