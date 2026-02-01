package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.TransactionType;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record Transaction(
        UUID id,
        Account account,
        TransactionType type,
        Category category,
        Amount amount,
        String description,
        LocalDate transactionDate,
        Instant createdAt,
        Instant updatedAt
) {
    public Transaction {
        Optional.ofNullable(category).orElseThrow(() -> new IllegalArgumentException("Category is required"));
        Optional.ofNullable(account).orElseThrow(() -> new IllegalArgumentException("Account is required"));
        Optional.ofNullable(type).orElseThrow(() -> new IllegalArgumentException("Transaction type is required"));
    }
}
