package com.fabiankevin.app.web.controllers.dtos.shopping_list;

import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to create a shopping list")
public record CreateShoppingListRequest(
        @NotBlank(message = "name is required")
        @Schema(description = "Shopping list name", example = "Groceries")
        String name,

        @Schema(description = "Category", example = "Food")
        String category,

        @Schema(description = "Description", example = "Weekly groceries")
        String description,

        @Schema(description = "Budget", example = "200.0")
        double budget
) {
    public CreateShoppingListCommand toCommand(UUID userId) {
        return CreateShoppingListCommand.builder()
                .name(name())
                .category(category())
                .description(description())
                .userId(userId)
                .budget(budget())
                .build();
    }
}
