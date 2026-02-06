package com.fabiankevin.app.models;

import com.fabiankevin.app.exceptions.InvalidAmountException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

public record Amount(BigDecimal value, Currency currency) {
    public Amount {
        Optional.ofNullable(currency)
                .orElseThrow(() -> new IllegalArgumentException("Currency is required"));
        Optional.ofNullable(value)
                .orElseThrow(() -> new IllegalArgumentException("Amount total is required"));
        if (value.signum() < 0) {
            throw new InvalidAmountException("Amount cannot be negative");
        }
    }

    public static Amount of(double value, Currency currency) {
        return new Amount(BigDecimal.valueOf(value), currency);
    }
}
