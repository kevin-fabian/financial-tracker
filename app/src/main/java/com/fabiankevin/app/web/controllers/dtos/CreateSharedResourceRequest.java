package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Resource to share into a space during creation")
public record CreateSharedResourceRequest(
    @NotNull(message = "Resource type is required")
    @Schema(description = "Type of the resource", example = "TRANSACTION")
    ResourceType type,

    @Schema(description = "Identifiers of the items to share", example = "[\"txn-1\", \"txn-2\"]")
    List<String> itemIds
) {
}
