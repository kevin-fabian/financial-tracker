package com.fabiankevin.app.models.enums;

import java.util.UUID;

public record User(
        UUID id,
        String firstName
) {
}
