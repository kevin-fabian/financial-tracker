package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Amount;

import java.math.BigDecimal;
import java.util.Currency;

public record AmountResponse(BigDecimal value, Currency currency) {
    public static AmountResponse from(Amount amount) {
        return new AmountResponse(amount.value(), amount.currency());
    }
}
