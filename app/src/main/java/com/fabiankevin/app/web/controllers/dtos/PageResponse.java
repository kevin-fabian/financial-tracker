package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Response DTO representing a paginated result")
public record PageResponse<T>(
        @Schema(description = "List of content items")
        List<T> content,
        @Schema(description = "Current page number (0-based)", example = "0")
        int page,
        @Schema(description = "Page size", example = "10")
        int size,
        @Schema(description = "Total number of elements", example = "100")
        long totalElements,
        @Schema(description = "Total number of pages", example = "10")
        int totalPages,
        @Schema(description = "Whether this is the last page", example = "false")
        boolean last,
        @Schema(description = "Whether this is the first page", example = "true")
        boolean first
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.content())
                .page(page.page())
                .size(page.size())
                .totalElements(page.totalElements())
                .totalPages(page.totalPages())
                .last(page.last())
                .first(page.first())
                .build();
    }
}
