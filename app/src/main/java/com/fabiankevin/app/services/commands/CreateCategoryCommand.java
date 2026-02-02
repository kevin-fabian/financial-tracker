package com.fabiankevin.app.services.commands;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CreateCategoryCommand(
        String name,
        UUID userId
) {
}
