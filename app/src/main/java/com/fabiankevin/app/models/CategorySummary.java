package com.fabiankevin.app.models;

import lombok.Builder;

@Builder(toBuilder = true)
public record CategorySummary(
        Category category,
        double totalAmount,
        double percentage,
        int totalTransactions) {
}
