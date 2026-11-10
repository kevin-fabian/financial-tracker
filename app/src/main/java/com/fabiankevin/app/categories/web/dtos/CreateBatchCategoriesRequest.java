package com.fabiankevin.app.categories.web.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating multiple categories in a batch")
public record CreateBatchCategoriesRequest(
        @NotEmpty(message = "At least one category request is required")
        @Size(max = 100, message = "Batch size must not exceed 100 categories")
        @Schema(description = "List of category creation requests")
        List<CreateCategoryRequest> categories
) {
}
