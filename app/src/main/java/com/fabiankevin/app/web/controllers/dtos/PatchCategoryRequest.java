package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.PatchCategoryCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for patching a category. All fields are optional.")
public record PatchCategoryRequest(
        @Schema(description = "Name of the category", example = "FOOD")
        String name,
        @Schema(description = "Transaction type of the category", example = "EXPENSE")
        TransactionType type,
        @Schema(description = "Icon name for the category")
        String iconName
) {
    public PatchCategoryCommand toCommand(UUID id, UUID userId) {
        return PatchCategoryCommand.builder()
                .id(id)
                .name(this.name())
                .type(this.type())
                .iconName(this.iconName)
                .userId(userId)
                .build();
    }
}

