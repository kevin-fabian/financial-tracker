package com.fabiankevin.app.accounts.persistence.entities;

import lombok.Builder;

@Builder(toBuilder = true)
public record AccountSummaryProjection(
        AccountEntity account,
        double totalBalance,
        int totalTransactions) {
}
