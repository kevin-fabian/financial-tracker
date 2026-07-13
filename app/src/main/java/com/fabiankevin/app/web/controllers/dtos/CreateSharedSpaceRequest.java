package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.services.commands.shared_space.AddSharedResourceCommand;
import com.fabiankevin.app.services.commands.shared_space.CreateSharedSpaceCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request to create a shared space")
public record CreateSharedSpaceRequest(
    @Schema(description = "Display name for the space", example = "Family 2026 Budget")
    String spaceName,

    @NotNull(message = "Sharing mode is required")
    @Schema(description = "Global sharing mode for the space", example = "MUTUAL_SHARING")
    SharingMode sharingMode,

    @Schema(description = "Resources to share into the space at creation time")
    List<CreateSharedResourceRequest> resources
) {
    public CreateSharedSpaceCommand toCommand(UUID ownerUserId) {
        List<AddSharedResourceCommand> commands = resources == null ? List.of() : resources.stream()
            .map(r -> AddSharedResourceCommand.builder()
                .type(r.type())
                .ownerUserId(ownerUserId)
                .itemIds(r.itemIds())
                .sharedByOwner(true)
                .build())
            .toList();

        return CreateSharedSpaceCommand.builder()
            .ownerUserId(ownerUserId)
            .spaceName(spaceName)
            .sharingMode(sharingMode)
            .resources(commands)
            .build();
    }
}
