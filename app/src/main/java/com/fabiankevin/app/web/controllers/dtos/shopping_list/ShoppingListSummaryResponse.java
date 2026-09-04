package com.fabiankevin.app.web.controllers.dtos.shopping_list;

import com.fabiankevin.app.models.Category;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.models.enums.ShoppingListStatus;
import com.fabiankevin.app.models.shopping_list.ShoppingListSummary;
import com.fabiankevin.app.web.controllers.dtos.CategoryResponse;
import com.fabiankevin.app.web.controllers.dtos.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response containing the created shopping list summary")
public record ShoppingListSummaryResponse(
        @Schema(description = "Unique identifier of the shopping list", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,

        @Schema(description = "Shopping list name", example = "Groceries")
        String name,

        @Schema(description = "Description", example = "Weekly groceries")
        String description,

        @Schema(description = "Status", example = "ACTIVE")
        ShoppingListStatus status,

        @Schema(description = "Budget", example = "200.0")
        double budget,

        @Schema(description = "Final amount", example = "185.0")
        Double finalAmount,

        @Schema(description = "Completion timestamp")
        Instant completedAt,

        @Schema(description = "Items in the shopping list")
        List<ShoppingItemResponse> items,

        @Schema(description = "Category details")
        CategoryResponse category,

        @Schema(description = "User details")
        UserResponse user,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
    public static ShoppingListSummaryResponse from(ShoppingListSummary summary) {
        User user = summary.user();
        Category category = summary.category();
        List<ShoppingItemResponse> items = summary.items().stream()
                .map(ShoppingItemResponse::from)
                .toList();
        return ShoppingListSummaryResponse.builder()
                .id(summary.id())
                .name(summary.name())
                .description(summary.description())
                .status(summary.status())
                .budget(summary.budget())
                .finalAmount(summary.finalAmount())
                .completedAt(summary.completedAt())
                .items(items)
                .category(CategoryResponse.from(category))
                .user(UserResponse.from(user))
                .createdAt(summary.createdAt())
                .updatedAt(summary.updatedAt())
                .build();
    }
}
