package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.queries.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request DTO for paginated queries")
public record PageRequest(
        @Schema(description = "Page number (0-based)", example = "0")
        int page,
        @Schema(description = "Page size", example = "10")
        int size,
        @Schema(description = "Sort field", example = "name")
        String sort,
        @Schema(description = "Sort direction", example = "ASC")
        String direction) {

    public PageQuery toQuery() {
        return new PageQuery(page, size, sort, direction);
    }
}