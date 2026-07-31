package com.fabiankevin.app.web.controllers.dtos.shopping_list;

import com.fabiankevin.app.services.shopping_list.commands.CreateShoppingListCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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

        @Schema(description = "Budget", example = "200.0")
        double budget,

        @Schema(description = "User IDs to share the list with")
        List<UUID> sharedWithUserIds
) {
    public CreateShoppingListCommand toCommand(UUID userId) {
        return CreateShoppingListCommand.builder()
                .name(name())
                .description(description())
                .userId(userId)
                .budget(budget())
                .sharedWithUserIds(sharedWithUserIds())
                .build();
    }
}
