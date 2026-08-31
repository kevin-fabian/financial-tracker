package com.fabiankevin.app.models;

import lombok.Builder;

@Builder(toBuilder = true)
public record AccountSummary(
        Account account,
        double totalBalance,
        int totalTransactions,
        User user) {
}
