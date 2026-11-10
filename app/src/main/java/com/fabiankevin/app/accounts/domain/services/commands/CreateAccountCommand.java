package com.fabiankevin.app.accounts.domain.services.commands;

import com.fabiankevin.app.accounts.domain.models.AccountType;
import lombok.Builder;

import java.util.Currency;
import java.util.UUID;

@Builder(toBuilder = true)
public record CreateAccountCommand(
        UUID id,
        String name,
        Currency currency,
        AccountType type,
        UUID userId) {
}
