package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
public record PatchTransactionRequest(
        UUID accountId,
        TransactionType type,
        String description,
        UUID categoryId,
        AmountRequest amount,
        LocalDate transactionDate
) {
    public PatchTransactionCommand toCommand(UUID id, UUID userId) {
        return PatchTransactionCommand.builder()
                .id(id)
                .accountId(this.accountId())
                .type(this.type())
                .description(this.description())
                .categoryId(this.categoryId())
                .amount(this.amount() != null ? this.amount().toAmount() : null)
                .transactionDate(this.transactionDate())
                .userId(userId)
                .build();
    }
}

