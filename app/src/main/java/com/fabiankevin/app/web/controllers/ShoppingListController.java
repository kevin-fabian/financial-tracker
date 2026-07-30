package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.services.ShoppingListService;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.CreateShoppingItemRequest;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.CreateShoppingListRequest;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.ShoppingItemResponse;
import com.fabiankevin.app.web.controllers.dtos.shopping_list.ShoppingListSummaryResponse;
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
    public List<ShoppingListSummaryResponse> createShoppingList
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
    public ShoppingListSummaryResponse create(
            @Valid @RequestBody CreateShoppingListRequest request,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        return ShoppingListSummaryResponse.from(shoppingListService.createShoppingList(request.toCommand(userId)));
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
}
