package com.fabiankevin.app.services.recurring_transactions.commands;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CreateRecurringTransactionCommand(
        UUID userId,
        String description,
        double amount,
        boolean variableAmount,
        UUID categoryId,
        UUID accountId,
        boolean noEndDate,
        int dayOfMonth,
        int durationMonths
) {
}
