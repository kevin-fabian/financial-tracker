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

        @Schema(description = "Account id associated with the transaction",
                exampleClasses = AccountResponse.class)
        AccountResponse account,

        @Schema(description = "Transaction type", example = "EXPENSE")
        String type,

        @Schema(description = "Category id associated with the transaction",
                exampleClasses = CategoryResponse.class)
        CategoryResponse category,

        @Schema(description = "Amount total and currency",
                exampleClasses = AmountResponse.class)
        AmountResponse amount,

        @Schema(description = "Transaction description", example = "Dinner with friends")
        String description,

        @Schema(description = "Date of transaction", example = "2025-02-01")
        LocalDate transactionDate,

        @Schema(description = "Timestamp when the transaction was created", example = "2025-02-01T18:30:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp when the transaction was last updated", example = "2025-02-01T18:30:00Z")
        Instant updatedAt,

        @Schema(description = "User who created the transaction",
                exampleClasses = UserResponse.class)
        UserResponse addedBy,

        @Schema(description = "User who last updated the transaction",
                exampleClasses = UserResponse.class)
        UserResponse updatedBy) {
    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .id(t.id())
                .account(AccountResponse.from(t.account()))
                .type(t.category().type().name())
                .category(CategoryResponse.from(t.category()))
                .amount(new AmountResponse(t.amount(), t.account().currency()))
                .description(t.description())
                .transactionDate(t.transactionDate())
                .createdAt(t.createdAt())
                .updatedAt(t.updatedAt())
                .addedBy(UserResponse.from(t.addedBy()))
                .updatedBy(UserResponse.from(t.updatedBy()))
                .build();
    }
}
