package com.fabiankevin.app.sync_devices.web.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for pulling sync changes")
public record SyncPullChangesRequest(
        @NotNull(message = "Cursors are required")
        @NotEmpty(message = "At least one cursor is required")
        @Valid
        @Schema(description = "List of cursors for each user/device")
        List<SyncCursorQueryRequest> cursors) {
}
