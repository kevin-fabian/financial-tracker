package com.fabiankevin.app.services.commands;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
public record AddTransactionCommand(
        double amount,
        String description,
        LocalDate transactionDate,
        UUID categoryId,
        UUID accountId,
        UUID userId,
        UUID recurringTransactionId) {
}
