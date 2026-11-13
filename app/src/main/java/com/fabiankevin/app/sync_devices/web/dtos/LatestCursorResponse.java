package com.fabiankevin.app.sync_devices.web.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder(toBuilder = true)
@Schema(description = "Response DTO for the latest sync cursor")
public record LatestCursorResponse(
        @NotNull(message = "Latest cursor is required")
        @Schema(description = "The latest sequence ID for the user's device", example = "150")
        Long latestCursor) {

    public static LatestCursorResponse from(Long latestCursor) {
        return LatestCursorResponse.builder()
                .latestCursor(latestCursor)
                .build();
    }

    public static LatestCursorResponse empty() {
        return LatestCursorResponse.builder()
                .latestCursor(0L)
                .build();
    }
}
