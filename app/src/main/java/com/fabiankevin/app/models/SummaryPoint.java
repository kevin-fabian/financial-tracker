package com.fabiankevin.app.models;

import java.math.BigDecimal;

public record SummaryPoint(
        String label,
        BigDecimal total
) {
}
