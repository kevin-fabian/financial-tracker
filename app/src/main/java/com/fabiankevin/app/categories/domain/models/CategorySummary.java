package com.fabiankevin.app.categories.domain.models;

import lombok.Builder;

@Builder(toBuilder = true)
public record CategorySummary(
        Category category,
        double totalAmount,
        double percentage,
        int totalTransactions) {
}
