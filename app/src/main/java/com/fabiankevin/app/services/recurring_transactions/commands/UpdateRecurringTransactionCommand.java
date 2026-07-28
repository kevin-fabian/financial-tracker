package com.fabiankevin.app.services.recurring_transactions.commands;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record UpdateRecurringTransactionCommand(
        UUID userId,
        UUID id,
        String description,
        Double amount,
        Boolean variableAmount,
        UUID categoryId,
        UUID accountId,
        Boolean noEndDate,
        Integer dayOfMonth,
        Integer durationMonths
) {
}
