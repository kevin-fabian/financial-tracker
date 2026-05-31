package com.fabiankevin.app.services.commands;

import com.fabiankevin.app.models.IconData;
import com.fabiankevin.app.models.enums.TransactionType;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CreateCategoryCommand(
        String name,
        TransactionType type,
        IconData icon,
        UUID userId
) {
}
