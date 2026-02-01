package com.fabiankevin.app.persistence.entities.embeddables;

import com.fabiankevin.app.models.Amount;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
public class AmountEmbeddable {
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    public static AmountEmbeddable from(Amount amount) {
        return AmountEmbeddable.builder()
                .amount(amount.value())
                .currency(amount.currency().getCurrencyCode())
                .build();
    }

    public Amount toModel() {
        return new Amount(amount, Currency.getInstance(currency));
    }
}