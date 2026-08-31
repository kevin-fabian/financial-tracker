package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.CategorySummary;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.web.controllers.dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/categories", version = "v1")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(
            summary = "Retrieves paginated category summaries",
            description = "Retrieves a paginated list of category summaries with aggregated transaction data based on the provided pagination parameters",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resources retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping
    public PageResponse<CategorySummaryResponse> getCategorySummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) TransactionType type,
            JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Page<CategorySummary> summaries = categoryService.getCategorySummariesByPageQuery(new PageQuery(page, size, sort, direction), userId, type);

        return PageResponse.from(Page.<CategorySummaryResponse>builder()
                .content(summaries.content().stream().map(CategorySummaryResponse::from).toList())
                .page(summaries.page())
                .size(summaries.size())
                .totalElements(summaries.totalElements())
                .totalPages(summaries.totalPages())
                .last(summaries.last())
                .first(summaries.first())
                .build());
    }

    @Operation(
            summary = "Retrieve a category",
            description = "Retrieves a transaction category by specified ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resource is retrieved successfully",
                            content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping("/{id}")
    public CategoryResponse getCategory(@PathVariable UUID id, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Category category = categoryService.getCategoryById(id, userId);
        return CategoryResponse.from(category);
    }

    @Operation(
            summary = "Create a new category",
            description = "Creates a new transaction category and returns the created object",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Resource created successfully",
                            content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Category createdCategory = categoryService.createCategory(request.toCommand(userId));
        CategoryResponse response = CategoryResponse.from(createdCategory);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Patch a category",
            description = "Updates provided fields of a category and returns the updated object",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resource updated successfully",
                            content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PatchMapping("/{categoryId}")
    public CategoryResponse patchCategory(@PathVariable UUID categoryId, @RequestBody @Valid PatchCategoryRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Category updated = categoryService.patchCategory(request.toCommand(categoryId, userId));
        return CategoryResponse.from(updated);
    }

    @Operation(
            summary = "Disable a category",
            description = "Disables a transaction category by specified ID. This operation is idempotent.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content - Category disabled successfully"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping("/{categoryId}/disable")
    public ResponseEntity<Void> disableCategory(@PathVariable UUID categoryId, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        categoryService.disableCategory(categoryId, userId);
        return ResponseEntity.noContent().build();
    }
}
