package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.models.enums.TransactionType;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CategorySummaryProjection(
        UUID id,
        String name,
        TransactionType type,
        UUID userId,
        String icon,
        double amount,
        int totalTransactions
) {
}
