package com.fabiankevin.app.web.controllers.dtos.shopping_list;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.models.shopping_list.ShoppingItemSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response containing the added shopping item")
public record ShoppingItemResponse(
        @Schema(description = "Unique identifier of the item", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "Item name", example = "Milk")
        String name,

        @Schema(description = "Category", example = "Dairy")
        String category,

        @Schema(description = "Quantity", example = "2.0")
        double quantity,

        @Schema(description = "Unit", example = "liters")
        String unit,

        @Schema(description = "Price", example = "3.5")
        double price,

        @Schema(description = "Whether the item has been purchased", example = "false")
        boolean purchased,

        @Schema(description = "Priority", example = "HIGH")
        ItemPriority priority,

        @Schema(description = "Notes", example = "Whole milk")
        String notes,

        @Schema(description = "First name of the item creator", example = "Kevin")
        String addedByFirstName,

        @Schema(description = "Last name of the item creator", example = "Fabian")
        String addedByLastName,

        @Schema(description = "Initial of the item creator", example = "KF")
        String addedByInitial,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
    public static ShoppingItemResponse from(ShoppingItemSummary summary) {
        User user = summary.addedBy();
        return ShoppingItemResponse.builder()
                .id(summary.id())
                .name(summary.name())
                .category(summary.category())
                .quantity(summary.quantity())
                .unit(summary.unit())
                .price(summary.price())
                .purchased(summary.purchased())
                .priority(summary.priority())
                .notes(summary.notes())
                .addedByFirstName(user != null ? user.firstName() : null)
                .addedByLastName(user != null ? user.lastName() : null)
                .addedByInitial(user != null ? user.initial() : null)
                .createdAt(summary.createdAt())
                .updatedAt(summary.updatedAt())
                .build();
    }
}
