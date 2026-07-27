package com.fabiankevin.app.services.recurring_transactions.commands;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CreateRecurringTransactionCommand(
        UUID userId,
        String description,
        double amount,
        UUID categoryId,
        UUID accountId,
        int dayOfMonth,
        int durationMonths
) {
}
