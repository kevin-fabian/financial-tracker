package com.fabiankevin.app.models;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record AccountSummary(
        Account account,
        double totalBalance,
        int totalTransactions,
        UUID localId) {
}
