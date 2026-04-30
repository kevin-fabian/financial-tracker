package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating a category")
public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Schema(description = "Name of the category", example = "FOOD")
        String name,
        @NotNull(message = "Type is required")
        @Schema(description = "Transaction type of the category", example = "EXPENSE")
        TransactionType type
) {
    public CreateCategoryCommand toCommand(UUID userId) {
        return CreateCategoryCommand.builder()
                .name(this.name())
                .type(this.type())
                .userId(userId)
                .build();
    }
}
