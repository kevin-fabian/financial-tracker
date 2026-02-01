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
import java.util.Optional;

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
        if (amount == null) return null;
        return AmountEmbeddable.builder()
                .amount(amount.value())
                .currency(Optional.ofNullable(amount.currency()).map(Currency::getCurrencyCode).orElse(null))
                .build();
    }

    public Amount toModel() {
        return new Amount(
                this.amount,
                Optional.ofNullable(this.currency).map(Currency::getInstance).orElse(null)
        );
    }
}