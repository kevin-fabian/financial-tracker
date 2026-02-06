package com.fabiankevin.app.services.commands;

import com.fabiankevin.app.models.Amount;
import com.fabiankevin.app.models.enums.TransactionType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
public record PatchTransactionCommand(
        UUID id,
        UUID accountId,
        TransactionType type,
        String description,
        UUID categoryId,
        Amount amount,
        LocalDate transactionDate,
        UUID userId
) {
}

