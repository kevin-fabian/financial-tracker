package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.web.controllers.dtos.CategoryResponse;
import com.fabiankevin.app.web.controllers.dtos.CreateCategoryRequest;
import com.fabiankevin.app.web.controllers.dtos.PageRequest;
import com.fabiankevin.app.web.controllers.dtos.PageResponse;
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
            summary = "Retrieves paginated categories",
            description = "Retrieves a paginated list of transaction categories based on the provided pagination parameters",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resources retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping
    public PageResponse<Category> getPaginatedCategories(@RequestParam PageRequest pageRequest, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        Page<Category> categoriesByPageQuery = categoryService.getCategoriesByPageQuery(pageRequest.toQuery(), userId);
        return PageResponse.from(categoriesByPageQuery);
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
            summary = "Delete a category",
            description = "Deletes a transaction category by specified ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content - Resource deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        categoryService.deleteCategoryById(id, userId);
        return ResponseEntity.noContent().build();
    }
}
