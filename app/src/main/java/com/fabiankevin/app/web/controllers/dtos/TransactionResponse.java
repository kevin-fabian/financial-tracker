package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO representing a transaction record")
public record TransactionResponse(
        @Schema(description = "Unique identifier of the transaction", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "Account id associated with the transaction")
        AccountResponse account,

        @Schema(description = "Transaction type", example = "EXPENSE")
        String type,

        @Schema(description = "Category id associated with the transaction")
        CategoryResponse category,

        @Schema(description = "Amount value and currency")
        AmountResponse amount,

        @Schema(description = "Transaction description")
        String description,

        @Schema(description = "Date of transaction")
        LocalDate transactionDate,

        @Schema(description = "Timestamp when the transaction was created")
        Instant createdAt,

        @Schema(description = "Timestamp when the transaction was last updated")
        Instant updatedAt) {
    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .id(t.id())
                .account(AccountResponse.from(t.account()))
                .type(t.type().name())
                .category(CategoryResponse.from(t.category()))
                .amount(AmountResponse.from(t.amount()))
                .description(t.description())
                .transactionDate(t.transactionDate())
                .createdAt(t.createdAt())
                .updatedAt(t.updatedAt())
                .build();
    }
}
