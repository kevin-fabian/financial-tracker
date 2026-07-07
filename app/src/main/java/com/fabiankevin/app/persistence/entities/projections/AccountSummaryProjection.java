package com.fabiankevin.app.persistence.entities.projections;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record AccountSummaryProjection(
        UUID id,
        String name,
        UUID userId,
        String currency,
        String type,
        boolean active,
        boolean system,
        double totalAmount,
        int totalTransactions) {
}
