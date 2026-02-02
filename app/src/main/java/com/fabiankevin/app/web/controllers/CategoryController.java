package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.services.CategoryService;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import com.fabiankevin.app.web.controllers.dtos.CategoryResponse;
import com.fabiankevin.app.web.controllers.dtos.CreateCategoryRequest;
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
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(
            summary = "Retrieve a category",
            description = "Retrieves a transaction category by specified ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resource is retrieved successfully",
                            content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found")
            }
    )
    @GetMapping("/{id}")
    public CategoryResponse getCategory(@PathVariable UUID id, JwtAuthenticationToken jwtAuthenticationToken) {
        log.debug("GET /categories/{}", id);
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
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input")
            }
    )
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request, JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        CreateCategoryCommand command = request.toCommand().toBuilder().userId(userId).build();
        Category createdCategory = categoryService.createCategory(command);
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
                    @ApiResponse(responseCode = "404", description = "Not Found - Resource not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id, JwtAuthenticationToken jwtAuthenticationToken) {
        log.debug("DELETE /categories/{}", id);
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        categoryService.deleteCategoryById(id, userId);
        return ResponseEntity.noContent().build();
    }
}
