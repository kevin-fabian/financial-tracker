package com.fabiankevin.app.web.controllers.dtos.shopping_list;

import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingItemCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to add an item to a shopping list")
public record CreateShoppingItemRequest(
        @NotBlank(message = "name is required")
        @Size(max = 128, message = "name must not exceed 128 characters")
        @Schema(description = "Item name", example = "Milk")
        String name,

        @NotBlank(message = "category is required")
        @Size(max = 128, message = "category must not exceed 128 characters")
        @Schema(description = "Category", example = "Dairy")
        String category,

        @Schema(description = "Quantity", example = "2.0")
        double quantity,

        @Size(max = 36, message = "unit must not exceed 36 characters")
        @Schema(description = "Unit", example = "liters")
        String unit,

        @Schema(description = "Price", example = "3.5")
        double price,

        @Size(max = 128, message = "notes must not exceed 128 characters")
        @Schema(description = "Notes", example = "Whole milk")
        String notes,

        @NotNull(message = "priority is required")
        @Schema(description = "Priority", example = "HIGH")
        ItemPriority priority
) {
    public CreateShoppingItemCommand toCommand(UUID shoppingListId, UUID addedBy) {
        return CreateShoppingItemCommand.builder()
                .name(name())
                .category(category())
                .quantity(quantity())
                .unit(unit())
                .price(price())
                .notes(notes())
                .addedBy(addedBy)
                .shoppingListId(shoppingListId)
                .priority(priority())
                .build();
    }
}
