package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.services.ShoppingListService;
import com.fabiankevin.app.services.shopping_list.commands.DeleteShoppingItemCommand;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/shopping-lists", version = "v1")
public class ShoppingListController {
    private final ShoppingListService shoppingListService;

    @Operation(
            summary = "Retrieve shopping lists",
            description = "Retrieves a list of shopping list summaries for the authenticated user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Resources retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ShoppingListSummaryResponse.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @GetMapping
    public List<ShoppingListSummaryResponse> getShoppingLists
            (JwtAuthenticationToken jwtAuthenticationToken) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return shoppingListService.getShoppingListsByUserId(userId).stream()
                .map(ShoppingListSummaryResponse::from)
                .toList();
    }

    @Operation(
            summary = "Create a new shopping list",
            description = "Creates a new shopping list for the authenticated user and returns the created summary.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Shopping list created successfully",
                            content = @Content(schema = @Schema(implementation = ShoppingListSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingListSummaryResponse createShoppingList(
            @Valid @RequestBody CreateShoppingListRequest request,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return ShoppingListSummaryResponse.from(shoppingListService.createShoppingList(request.toCommand(userId)));
    }

    @Operation(
            summary = "Complete a shopping list",
            description = "Marks the specified shopping list as completed with the final amount.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Shopping list completed successfully",
                            content = @Content(schema = @Schema(implementation = ShoppingListSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Shopping list not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping("/{id}/complete")
    public ShoppingListSummaryResponse completeShoppingList(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteShoppingListRequest request,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return ShoppingListSummaryResponse.from(shoppingListService.completeShoppingList(request.toCommand(id, userId)));
    }

    @Operation(
            summary = "Update a shopping list",
            description = "Partially updates the specified shopping list. All fields are optional.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Shopping list updated successfully",
                            content = @Content(schema = @Schema(implementation = ShoppingListSummaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Shopping list not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PatchMapping("/{id}")
    public ShoppingListSummaryResponse updateShoppingList(
            @PathVariable UUID id,
            @Valid @RequestBody PatchShoppingListRequest request,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return ShoppingListSummaryResponse.from(shoppingListService.updateShoppingList(request.toCommand(id, userId)));
    }

    @Operation(
            summary = "Add an item to a shopping list",
            description = "Adds a new item to the specified shopping list and returns the created item.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created - Item added successfully",
                            content = @Content(schema = @Schema(implementation = ShoppingItemResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Shopping list not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingItemResponse addShoppingItem(
            @PathVariable UUID id,
            @Valid @RequestBody CreateShoppingItemRequest request,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return ShoppingItemResponse.from(shoppingListService.addShoppingItem(request.toCommand(id, userId)));
    }

    @Operation(
            summary = "Update a shopping item",
            description = "Partially updates a shopping item in the specified shopping list. All fields are optional.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Item updated successfully",
                            content = @Content(schema = @Schema(implementation = ShoppingItemResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Shopping list or item not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @PatchMapping("/{id}/items/{itemId}")
    public ShoppingItemResponse updateShoppingItem(
            @PathVariable("id") UUID shoppingListId,
            @PathVariable UUID itemId,
            @Valid @RequestBody PatchShoppingItemRequest request,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return ShoppingItemResponse.from(shoppingListService.updateShoppingItem(request.toCommand(shoppingListId, itemId, userId)));
    }

    @Operation(
            summary = "Delete a shopping item",
            description = "Deletes a shopping item from the specified shopping list.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No Content - Item deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Not Found - Shopping list or item not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
            }
    )
    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShoppingItem(
            @PathVariable("id") UUID shoppingListId,
            @PathVariable UUID itemId,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        shoppingListService.deleteShoppingItem(DeleteShoppingItemCommand.builder()
                .shoppingListId(shoppingListId)
                .itemId(itemId)
                .userId(userId)
                .build());
    }
}
