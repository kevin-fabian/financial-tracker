package com.fabiankevin.app.services.commands;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record PatchCategoryCommand(
        UUID id,
        String name,
        UUID userId
) {
}

