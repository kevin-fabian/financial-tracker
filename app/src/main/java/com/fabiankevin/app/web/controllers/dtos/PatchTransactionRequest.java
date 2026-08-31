package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
public record PatchTransactionRequest(
        UUID accountId,
        String description,
        UUID categoryId,
        @Schema(description = "Transaction amount", example = "100.00")
        Double amount,
        LocalDate transactionDate
) {
    public PatchTransactionCommand toCommand(UUID id, UUID userId) {
        return PatchTransactionCommand.builder()
                .id(id)
                .accountId(this.accountId)
                .description(this.description)
                .categoryId(this.categoryId)
                .amount(this.amount)
                .transactionDate(this.transactionDate)
                .userId(userId)
                .build();
    }
}

