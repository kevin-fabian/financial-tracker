package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.IconData;
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
        @Schema(description = "Icon for the category")
        IconResponse icon
) {
    public PatchCategoryCommand toCommand(UUID id, UUID userId) {
        IconData iconData = this.icon != null ? toIconData(this.icon) : null;
        return PatchCategoryCommand.builder()
                .id(id)
                .name(this.name())
                .type(this.type())
                .icon(iconData)
                .userId(userId)
                .build();
    }

    private IconData toIconData(IconResponse icon) {
        return IconData.builder()
                .id(icon.id())
                .codePoint(icon.codePoint())
                .fontFamily(icon.fontFamily())
                .build();
    }
}

