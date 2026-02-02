package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating a category")
public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Schema(description = "Name of the category", example = "FOOD")
        String name
) {
    public CreateCategoryCommand toCommand(UUID userId) {
        return CreateCategoryCommand.builder()
                .name(this.name())
                .userId(userId)
                .build();
    }
}
