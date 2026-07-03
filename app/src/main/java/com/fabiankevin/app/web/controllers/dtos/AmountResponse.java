package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Amount;
import lombok.Builder;

import java.util.Currency;

@Builder
public record AmountResponse(double value, Currency currency) {
    public static AmountResponse from(Amount amount) {
        return new AmountResponse(amount.value(), amount.currency());
    }
}
