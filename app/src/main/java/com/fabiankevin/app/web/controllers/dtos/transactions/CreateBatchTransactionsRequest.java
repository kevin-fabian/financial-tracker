package com.fabiankevin.app.web.controllers.dtos.transactions;

import com.fabiankevin.app.services.commands.AddTransactionCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating multiple transactions in a batch")
public record CreateBatchTransactionsRequest(
        @NotEmpty(message = "At least one transaction request is required")
        @Size(max = 100, message = "Batch size must not exceed 100 transactions")
        @Schema(description = "List of transaction creation requests")
        List<CreateTransactionRequest> transactions
) {
    public List<AddTransactionCommand> toCommands(UUID userId) {
        return transactions.stream()
                .map(r -> r.toCommand(userId))
                .toList();
    }
}
