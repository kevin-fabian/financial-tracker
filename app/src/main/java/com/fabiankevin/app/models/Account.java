package com.fabiankevin.app.models;

import lombok.Builder;

import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
public record Account(
        UUID id,
        String name,
        UUID userId,
        Currency currency,
        Instant createdAt,
        Instant updatedAt
) {
    public Account {
        Optional.ofNullable(userId).orElseThrow(() -> new IllegalArgumentException("User ID is required"));
        Optional.ofNullable(currency).orElseThrow(() -> new IllegalArgumentException("Currency is required"));
    }

//    public double getBalance() {
//        return sumTransactionAmountByType(INCOME) - sumTransactionAmountByType(EXPENSE);
//    }
//
//    private double sumTransactionAmountByType(TransactionType type){
//        return transactions.stream()
//                .filter(transaction -> type == transaction.type())
//                .map(Transaction::amount)
//                .flatMapToDouble(amount -> DoubleStream.builder().add(amount.value().doubleValue()).build())
//                .sum();
//    }
}
