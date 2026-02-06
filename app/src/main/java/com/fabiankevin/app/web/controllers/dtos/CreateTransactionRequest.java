package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Amount;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.AddTransactionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating a transaction")
public record CreateTransactionRequest(
        @NotNull
        @Schema(description = "Amount object", required = true)
        Amount amount,

        @NotNull
        @Schema(description = "Transaction type", example = "EXPENSE", required = true)
        TransactionType type,

        @Schema(description = "Transaction description", example = "Dinner with friends")
        String description,

        @NotNull
        @Schema(description = "Date of transaction", example = "2025-02-01", required = true)
        LocalDate transactionDate,

        @NotNull
        @Schema(description = "Category id", example = "d290f1ee-6c54-4b01-90e6-d701748f0851", required = true)
        UUID categoryId,

        @NotNull
        @Schema(description = "Account id", example = "d290f1ee-6c54-4b01-90e6-d701748f0852", required = true)
        UUID accountId
) {
    public AddTransactionCommand toCommand(UUID userId) {
        return AddTransactionCommand.builder()
                .amount(this.amount())
                .type(this.type())
                .description(this.description())
                .transactionDate(this.transactionDate())
                .categoryId(this.categoryId())
                .accountId(this.accountId())
                .userId(userId)
                .build();
    }
}
