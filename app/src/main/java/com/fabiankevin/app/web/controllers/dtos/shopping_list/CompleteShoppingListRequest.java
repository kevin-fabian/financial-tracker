package com.fabiankevin.app.web.controllers.dtos.shopping_list;

import com.fabiankevin.app.services.shopping_list.commands.CompleteShoppingListCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to complete a shopping list")
public record CompleteShoppingListRequest(
        @NotNull(message = "finalAmount is required")
        @Schema(description = "Final amount", example = "185.0")
        Double finalAmount
) {
    public CompleteShoppingListCommand toCommand(UUID shoppingListId, UUID userId) {
        return CompleteShoppingListCommand.builder()
                .shoppingListId(shoppingListId)
                .finalAmount(finalAmount())
                .userId(userId)
                .build();
    }
}
