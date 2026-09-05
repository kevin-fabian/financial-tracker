package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Amount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Currency;

@Builder
@Schema(description = "Response DTO representing an amount with value and currency")
public record AmountResponse(
        @Schema(description = "Amount value", example = "100.00")
        double value,
        @Schema(description = "Currency code", example = "USD")
        Currency currency) {
    public static AmountResponse from(Amount amount) {
        return new AmountResponse(amount.value(), amount.currency());
    }
}
