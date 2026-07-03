package com.fabiankevin.app.models;

import com.fabiankevin.app.exceptions.InvalidAmountException;

import java.util.Currency;
import java.util.Optional;

public record Amount(double value, Currency currency) {
    public Amount {
        Optional.ofNullable(currency)
                .orElseThrow(() -> new IllegalArgumentException("Currency is required"));
        if (value < 0) {
            throw new InvalidAmountException("Amount cannot be negative");
        }
    }

    public static Amount of(double value, Currency currency) {
        return new Amount(value, currency);
    }
}
