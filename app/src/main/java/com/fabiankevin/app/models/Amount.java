package com.fabiankevin.app.models;

import com.fabiankevin.app.exceptions.InvalidAmountException;

import java.util.Currency;
import java.util.Objects;

public record Amount(double value, Currency currency) {
    public Amount {
        Objects.requireNonNull(currency, "Currency is required");
        if (value < 0) {
            throw new InvalidAmountException("Amount cannot be negative");
        }
    }

    public static Amount of(double value, Currency currency) {
        return new Amount(value, currency);
    }

    public static Amount of(double value, String currency) {
        return new Amount(value, Currency.getInstance(currency));
    }

    public static Amount of(double value) {
        return new Amount(value, Currency.getInstance("PHP"));
    }
}
