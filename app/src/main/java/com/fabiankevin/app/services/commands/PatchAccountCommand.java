package com.fabiankevin.app.services.commands;

import lombok.Builder;

import java.util.Currency;
import java.util.UUID;

@Builder(toBuilder = true)
public record PatchAccountCommand(
        UUID id,
        String name,
        Currency currency,
        UUID userId
) {
}

