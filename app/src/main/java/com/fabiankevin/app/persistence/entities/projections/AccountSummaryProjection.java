package com.fabiankevin.app.persistence.entities.projections;

import com.fabiankevin.app.persistence.entities.AccountEntity;
import lombok.Builder;

@Builder(toBuilder = true)
public record AccountSummaryProjection(
        AccountEntity account,
        double totalBalance,
        int totalTransactions) {
}
