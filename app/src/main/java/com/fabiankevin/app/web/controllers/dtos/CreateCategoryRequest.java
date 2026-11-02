package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating a category")
public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 128, message = "Name must not exceed 128 characters")
        @Schema(description = "Name of the category", example = "FOOD")
        String name,
        @NotNull(message = "Type is required")
        @Schema(description = "Transaction type of the category", example = "EXPENSE")
        TransactionType type,
        @Schema(description = "Icon for the category", example = "attach_money")
        String icon,
        @Schema(description = "Client-facing identifier that may differ from the internal UUID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID localId
) {
    public CreateCategoryCommand toCommand(UUID userId) {
        return CreateCategoryCommand.builder()
                .name(this.name)
                .type(this.type)
                .icon(this.icon)
                .userId(userId)
                .localId(this.localId)
                .build();
    }
}
