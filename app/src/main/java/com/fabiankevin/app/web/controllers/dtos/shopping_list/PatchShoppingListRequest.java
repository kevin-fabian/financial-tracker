package com.fabiankevin.app.web.controllers.dtos.shopping_list;

import com.fabiankevin.app.services.shopping_list.commands.UpdateShoppingListCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to update a shopping list")
public record PatchShoppingListRequest(
        @Size(max = 64, message = "name must not exceed 64 characters")
        @Schema(description = "Shopping list name", example = "Groceries")
        String name,

        @Size(max = 128, message = "description must not exceed 128 characters")
        @Schema(description = "Description", example = "Weekly groceries")
        String description,

        @Schema(description = "Budget", example = "200.0")
        Double budget,

        @Schema(description = "User IDs to share the list with",
                example = "[\"d290f1ee-6c54-4b01-90e6-d701748f0852\"]")
        List<UUID> sharedWithUserIds,

        @Schema(description = "Category ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId
) {
    public UpdateShoppingListCommand toCommand(UUID shoppingListId, UUID userId) {
        return UpdateShoppingListCommand.builder()
                .shoppingListId(shoppingListId)
                .name(name())
                .description(description())
                .budget(budget())
                .sharedWithUserIds(sharedWithUserIds())
                .categoryId(categoryId())
                .userId(userId)
                .build();
    }
}
