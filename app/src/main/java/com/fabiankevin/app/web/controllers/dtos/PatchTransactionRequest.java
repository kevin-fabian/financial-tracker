package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.PatchTransactionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching a transaction. All fields are optional.")
public record PatchTransactionRequest(
        @Schema(description = "Account id", example = "d290f1ee-6c54-4b01-90e6-d701748f0852")
        UUID accountId,
        @Schema(description = "Transaction description", example = "Updated dinner description")
        String description,
        @Schema(description = "Category id", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId,
        @Schema(description = "Transaction amount", example = "100.00")
        @DecimalMin(value = "0.01", inclusive = false, message = "amount must be greater than zero")
        Double amount,
        @Schema(description = "Date of transaction", example = "2025-02-01")
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

