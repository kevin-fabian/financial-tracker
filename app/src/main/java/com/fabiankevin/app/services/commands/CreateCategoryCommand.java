package com.fabiankevin.app.services.commands;

import lombok.Builder;

@Builder(toBuilder = true)
public record CreateCategoryCommand(
        String name
) {
}
