package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.SyncStatus;
import com.fabiankevin.app.services.commands.CreateSyncStateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating an initial sync state")
public record CreateSyncStateRequest(
        @Size(max = 128, message = "User device must not exceed 128 characters")
        @Schema(description = "Identifier of the user's device", example = "iphone-15")
        String userDevice,
        @NotNull(message = "Resource type is required")
        @Schema(description = "Type of resource being synced", example = "ACCOUNTS")
        ResourceType resourceType,
        @Schema(description = "Initial sync status", example = "PENDING")
        SyncStatus syncStatus) {

    public CreateSyncStateCommand toCommand(UUID userId) {
        return CreateSyncStateCommand.builder()
                .userId(userId)
                .userDevice(this.userDevice)
                .resourceType(this.resourceType)
                .syncStatus(this.syncStatus != null ? this.syncStatus : SyncStatus.PENDING)
                .build();
    }
}
