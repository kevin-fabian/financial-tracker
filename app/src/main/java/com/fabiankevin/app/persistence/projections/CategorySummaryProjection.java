package com.fabiankevin.app.persistence.projections;

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
        boolean active,
        boolean system,
        double amount,
        int totalTransactions
) {
}
