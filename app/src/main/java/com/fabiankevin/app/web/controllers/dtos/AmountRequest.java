package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Amount;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Currency;

@Builder
public record AmountRequest(BigDecimal value, Currency currency) {
    public Amount toAmount() {
        return new Amount(value, currency);
    }
}
