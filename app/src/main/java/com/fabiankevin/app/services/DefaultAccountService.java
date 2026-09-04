package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.exceptions.AccountAlreadyExistException;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DefaultAccountService implements AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserClient userClient;
    private final PartyService partyService;

    @Override
    public Account getAccountById(UUID id, UUID userId) {
        Account account = accountRepository.findById(id)
                .filter(a -> a.user().id().equals(userId))
                .orElseThrow(AccountNotFoundException::new);
        Account.AccountBuilder builder = account.toBuilder();
        userClient.getUsersByIds(List.of(userId)).stream().findFirst()
                .ifPresent(builder::user);

        return builder.build();
    }

    @Transactional
    @Override
    public Account createAccount(CreateAccountCommand command) {
        return accountRepository.findByNameAndTypeAndUserId(command.name(), command.type(), command.userId())
                .map(this::reactivateAccount)
                .orElseGet(() -> createNewAccount(command));
    }

    private Account createNewAccount(CreateAccountCommand command) {
        Account account = Account.builder()
                .name(command.name())
                .active(true)
                .user(User.of(command.userId()))
                .currency(command.currency())
                .type(command.type())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return accountRepository.save(account);
    }

    private Account reactivateAccount(Account existingAccount) {
        if (existingAccount.active()) {
            throw new AccountAlreadyExistException("Account with the same name and type already exists for the user");
        }
        return accountRepository.save(existingAccount.toBuilder()
                .active(true)
                .updatedAt(Instant.now())
                .build());
    }

    @Transactional
    @Override
    public Account patchAccount(PatchAccountCommand command) {
        UUID id = command.id();
        UUID userId = command.userId();

        Account existing = accountRepository.findById(id)
                .filter(a -> a.user().id().equals(userId))
                .orElseThrow(AccountNotFoundException::new);

        String newName = command.name();
        Currency newCurrency = command.currency();
        AccountType newType = command.type();

        Account.AccountBuilder builder = existing.toBuilder()
                .updatedAt(Instant.now());

        Optional.ofNullable(newName)
                .filter(n -> !n.isBlank())
                .ifPresent(builder::name);
        Optional.ofNullable(newCurrency)
                .ifPresent(builder::currency);
        Optional.ofNullable(newType)
                .ifPresent(builder::type);

        return accountRepository.save(builder.build());
    }

    @Transactional
    @Override
    public AccountSummary createAccountSummary(CreateAccountCommand command) {
        Account account = createAccount(command);
        return getAccountSummaryById(account.id(), command.userId());
    }

    @Transactional
    @Override
    public AccountSummary patchAccountSummary(PatchAccountCommand command) {
        Account account = patchAccount(command);
        return getAccountSummaryById(account.id(), command.userId());
    }

    private AccountSummary getAccountSummaryById(UUID accountId, UUID userId) {
        AccountSummary summary = accountRepository.findSummaryByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        return enrichWithUserData(List.of(summary)).getFirst();
    }

    @Transactional
    @Override
    public void deleteAccountById(UUID id, UUID userId) {
        accountRepository.findById(id)
                .filter(a -> a.user().id().equals(userId))
                .orElseThrow(AccountNotFoundException::new);

        accountRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void disableAccount(UUID id, UUID userId) {
        accountRepository.findById(id)
                .filter(a -> a.user().id().equals(userId))
                .ifPresentOrElse(
                        account -> {
                            long transactionCount = transactionRepository.countByAccountId(id);
                            if (transactionCount == 0) {
                                accountRepository.deleteById(id);
                            } else {
                                accountRepository.save(account.toBuilder().active(false).build());
                            }
                        },
                        () -> { throw new AccountNotFoundException(); }
                );
    }

    @Override
    public Page<Account> getAccountsByPageAndUserId(PageQuery query, UUID userId) {
        return accountRepository.getAccountsByPageAndUserId(query, userId);
    }

    @Override
    public Page<AccountSummary> getAccountSummariesByPageQuery(PageQuery query, UUID userId, LocalDate monthStart, LocalDate monthEnd) {
        List<UUID> userIds = partyService.getPartyMembersUserId(userId);
        Page<AccountSummary> summaries = accountRepository.findAllByPageQueryWithSummary(query, userIds, monthStart, monthEnd);
        return Page.<AccountSummary>builder()
                .content(enrichWithUserData(summaries.content()))
                .page(summaries.page())
                .size(summaries.size())
                .totalElements(summaries.totalElements())
                .totalPages(summaries.totalPages())
                .last(summaries.last())
                .first(summaries.first())
                .build();
    }

    private List<AccountSummary> enrichWithUserData(List<AccountSummary> summaries) {
        List<UUID> userIds = summaries.stream()
                .map(accountSummary -> accountSummary.account().user().id())
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return summaries;
        }

        var usersById = userClient.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(User::id, u -> u));

        return summaries.stream()
                .map(summary -> {
                    UUID userId = summary.account().user().id();
                    User user = usersById.get(userId);
                    return Optional.ofNullable(user)
                            .map(u -> summary.toBuilder()
                                    .user(User.builder()
                                            .id(userId)
                                            .firstName(u.firstName())
                                            .lastName(u.lastName())
                                            .build())
                                    .build())
                            .orElse(summary);
                })
                .toList();
    }

    @Transactional
    @Override
    public void deleteAllByUserId(UUID userId) {
        accountRepository.deleteAllByUserId(userId);
    }
}
