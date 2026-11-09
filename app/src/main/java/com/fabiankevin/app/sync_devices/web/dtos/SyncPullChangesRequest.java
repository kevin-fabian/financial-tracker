package com.fabiankevin.app.sync_devices.web.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
        @Schema(description = "List of cursors for each household member/device")
        List<SyncCursorQueryRequest> cursors,

        @NotNull(message = "Primary last pulled sequence is required")
        @Min(value = 0, message = "Primary last pulled sequence must be non-negative")
        @Schema(description = "The authenticated user's last pulled sequence (primary cursor)", example = "120")
        Long primaryLastPulledSequence) {
}
