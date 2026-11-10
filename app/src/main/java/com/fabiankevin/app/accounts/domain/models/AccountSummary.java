package com.fabiankevin.app.accounts.domain.models;

import lombok.Builder;

@Builder(toBuilder = true)
public record AccountSummary(
        Account account,
        double totalBalance,
        int totalTransactions) {
}
