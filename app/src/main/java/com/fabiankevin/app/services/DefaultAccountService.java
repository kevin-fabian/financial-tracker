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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DefaultAccountService implements AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserClient userClient;
    private final HouseholdService householdService;

    @Override
    public AccountSummary getAccountById(UUID id, UUID userId) {
        AccountSummary summary = accountRepository.findSummaryByIdAndUserId(id, userId)
                .orElseThrow(AccountNotFoundException::new);
        return enrichWithUserData(List.of(summary), userId).getFirst();
    }

    @Transactional
    @Override
    public void createBatchAccounts(List<CreateAccountCommand> commands, UUID userId) {
        if (commands.isEmpty()) {
            return;
        }

        List<Account> existing = accountRepository.findByNameAndTypeAndUserIdBatch(commands, userId);
        Map<String, Account> existingByKey = existing.stream()
                .collect(Collectors.toMap(
                        a -> a.name() + ":" + a.type().name(),
                        a -> a
                ));

        List<Account> toSave = commands.stream()
                .map(command -> {
                    Account existingAccount = existingByKey.get(command.name() + ":" + command.type().name());
                    if (existingAccount != null && existingAccount.id().equals(command.id())) {
                        return null;
                    }

                    return Account.builder()
                            .name(command.name())
                            .active(true)
                            .user(User.of(command.userId()))
                            .currency(command.currency())
                            .type(command.type())
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        accountRepository.saveAll(toSave);
    }

    @Transactional
    @Override
    public AccountSummary createAccount(CreateAccountCommand command) {
        Account account = accountRepository.findByNameAndTypeAndUserId(command.name(), command.type(), command.userId())
                .map(this::reactivateAccount)
                .orElseGet(() -> createNewAccount(command));
        return getAccountById(account.id(), command.userId());
    }

    private Account createNewAccount(CreateAccountCommand command) {
        Account account = Account.builder()
                .id(command.id())
                .name(command.name())
                .active(true)
                .user(User.of(command.userId()))
                .currency(command.currency())
                .type(command.type())
                .localId(command.localId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return accountRepository.save(account);
    }

    private Account reactivateAccount(Account existingAccount) {
        if (existingAccount.active()) {
            throw new AccountAlreadyExistException("Account with the same name and type already exists for the user");
        }

        return accountRepository.update(existingAccount.toBuilder()
                .active(true)
                .updatedAt(Instant.now())
                .build());
    }

    @Transactional
    @Override
    public AccountSummary patchAccount(PatchAccountCommand command) {
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

        Account updated = accountRepository.update(builder.build());
        return getAccountById(updated.id(), userId);
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
                                accountRepository.update(account.toBuilder().active(false).build());
                            }
                        },
                        () -> { throw new AccountNotFoundException(); }
                );
    }

    @Override
    public Page<AccountSummary> getAccountSummariesByPageQuery(PageQuery query, UUID userId, LocalDate monthStart, LocalDate monthEnd) {
        List<UUID> userIds = householdService.getHouseholdMembersUserIds(userId);
        Page<AccountSummary> summaries = accountRepository.findAllByPageQueryWithSummary(query, userIds, monthStart, monthEnd);
        return Page.<AccountSummary>builder()
                .content(enrichWithUserData(summaries.content(), userId))
                .page(summaries.page())
                .size(summaries.size())
                .totalElements(summaries.totalElements())
                .totalPages(summaries.totalPages())
                .last(summaries.last())
                .first(summaries.first())
                .build();
    }

    private List<AccountSummary> enrichWithUserData(List<AccountSummary> summaries, UUID jwtUserId) {
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
                            .map(u -> {
                                String displayName = accountNameWithOwner(userId, jwtUserId, u.firstName(), summary.account().name());
                                return summary.toBuilder()
                                        .account(summary.account().toBuilder()
                                                .name(displayName)
                                                .user(User.builder()
                                                        .id(userId)
                                                        .firstName(u.firstName())
                                                        .lastName(u.lastName())
                                                        .build())
                                                .build())
                                        .build();
                            })
                            .orElse(summary);
                })
                .toList();
    }

    private String accountNameWithOwner(UUID accountUserId, UUID jwtUserId, String ownerFirstName, String accountName) {
        if (accountUserId.equals(jwtUserId)) {
            return accountName;
        }
        return ownerFirstName + " - " + accountName;
    }

    @Transactional
    @Override
    public void deleteAllByUserId(UUID userId) {
        accountRepository.deleteAllByUserId(userId);
    }
}
