package com.fabiankevin.app.sync_devices.web.dtos;

import com.fabiankevin.app.sync_devices.domain.models.SyncScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "Response DTO for pulling sync changes")
public record SyncPullChangesResponse(
        @Valid
        @NotNull(message = "Scopes are required")
        @Schema(description = "Scoped list of changes per user")
        List<SyncScopeDto> scopes) {

    public static SyncPullChangesResponse from(List<SyncScope> scopes) {
        return SyncPullChangesResponse.builder()
                .scopes(scopes.stream()
                        .map(SyncScopeDto::from)
                        .toList())
                .build();
    }
}
