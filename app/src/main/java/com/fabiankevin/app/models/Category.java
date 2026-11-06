package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.commands.CreateCategoryCommand;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record Category(
        UUID id,
        String name,
        TransactionType type,
        UUID userId,
        String icon,
        boolean active,
        boolean system,
        Instant createdAt,
        Instant updatedAt) {
    public Category {
        Optional.ofNullable(name)
                .filter(n -> !n.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Category name is required"));
        Optional.ofNullable(type)
                .orElseThrow(() -> new IllegalArgumentException("Category type is required"));
    }

    public static Category from(String name, TransactionType type, UUID userId, String icon){
        return Category.builder()
                .name(name)
                .type(type)
                .userId(userId)
                .icon(icon)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static Category from(CreateCategoryCommand command) {
        return Category.builder()
                .id(command.id())
                .name(command.name())
                .type(command.type())
                .userId(command.userId())
                .icon(command.icon())
                .active(true)
                .system(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
