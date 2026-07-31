package com.fabiankevin.app.web.controllers.dtos.shopping_list;

import com.fabiankevin.app.models.enums.ItemPriority;
import com.fabiankevin.app.services.shopping_list.commands.UpdateShoppingItemCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to update a shopping item")
public record UpdateShoppingItemRequest(
        @Size(max = 128, message = "name must not exceed 128 characters")
        @Schema(description = "Item name", example = "Milk")
        String name,

        @Size(max = 128, message = "category must not exceed 128 characters")
        @Schema(description = "Category", example = "Dairy")
        String category,

        @Schema(description = "Quantity", example = "2.0")
        Double quantity,

        @Size(max = 36, message = "unit must not exceed 36 characters")
        @Schema(description = "Unit", example = "liters")
        String unit,

        @Schema(description = "Price", example = "3.5")
        Double price,

        @Size(max = 32, message = "notes must not exceed 32 characters")
        @Schema(description = "Notes", example = "Whole milk")
        String notes,

        @Schema(description = "Priority", example = "HIGH")
        ItemPriority priority
) {
    public UpdateShoppingItemCommand toCommand(UUID shoppingListId, UUID itemId) {
        return UpdateShoppingItemCommand.builder()
                .shoppingListId(shoppingListId)
                .itemId(itemId)
                .name(name())
                .category(category())
                .quantity(quantity())
                .unit(unit())
                .price(price())
                .notes(notes())
                .priority(priority())
                .build();
    }
}
