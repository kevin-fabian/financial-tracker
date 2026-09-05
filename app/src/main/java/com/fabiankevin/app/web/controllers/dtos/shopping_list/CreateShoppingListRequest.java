package com.fabiankevin.app.web.controllers.dtos.shopping_list;

import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to create a shopping list")
public record CreateShoppingListRequest(
        @NotBlank(message = "name is required")
        @Size(max = 64, message = "name must not exceed 64 characters")
        @Schema(description = "Shopping list name", example = "Groceries")
        String name,

        @Size(max = 128, message = "description must not exceed 128 characters")
        @Schema(description = "Description", example = "Weekly groceries")
        String description,

        @NotNull(message = "categoryId is required")
        @Schema(description = "Category ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId,

        @PositiveOrZero(message = "budget must be positive or zero")
        @Schema(description = "Budget", example = "200.0")
        double budget,

        @Schema(description = "User IDs to share the list with",
                example = "[\"d290f1ee-6c54-4b01-90e6-d701748f0852\"]")
        List<UUID> sharedWithUserIds
) {
    public CreateShoppingListCommand toCommand(UUID userId) {
        return CreateShoppingListCommand.builder()
                .name(name())
                .description(description())
                .categoryId(categoryId())
                .userId(userId)
                .budget(budget())
                .sharedWithUserIds(sharedWithUserIds())
                .build();
    }
}
