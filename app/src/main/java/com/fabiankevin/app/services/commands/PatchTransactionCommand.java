package com.fabiankevin.app.services.commands;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
public record PatchTransactionCommand(
        UUID id,
        UUID accountId,
        String description,
        UUID categoryId,
        double amount,
        LocalDate transactionDate,
        UUID userId
) {
}

