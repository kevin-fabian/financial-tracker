package com.fabiankevin.app.models;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CategorySummary(
        Category category,
        double totalAmount,
        double percentage,
        int totalTransactions,
        UUID localId) {
}
