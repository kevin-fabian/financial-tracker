package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.CategorySummary;
import com.fabiankevin.app.models.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO representing a category with aggregated summary data")
public record CategorySummaryResponse(
        @Schema(description = "Unique identifier of the category", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "Name of the category", example = "FOOD")
        String name,
        @Schema(description = "Transaction type of the category", example = "EXPENSE")
        TransactionType type,
        @Schema(description = "Icon for the category", example = "food")
        String icon,
        @Schema(description = "Whether the category is active", example = "true")
        boolean active,
        @Schema(description = "Total totalAmount for this category", example = "150.75")
        double totalAmount,
        @Schema(description = "Percentage of total transactions for this category", example = "25.5")
        double percentage,
        @Schema(description = "Total number of transactions in this category", example = "10")
        int totalTransactions
) {
    public static CategorySummaryResponse from(final CategorySummary categorySummary) {
        return CategorySummaryResponse.builder()
                .id(categorySummary.id())
                .name(categorySummary.name())
                .type(categorySummary.type())
                .icon(categorySummary.icon())
                .active(categorySummary.active())
                .totalAmount(categorySummary.totalAmount())
                .percentage(categorySummary.percentage())
                .totalTransactions(categorySummary.totalTransactions())
                .build();
    }
}
