package com.fabiankevin.app.web.controllers.dtos;

import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record UserCreatedEvent(UUID userId,
                               Metadata metadata) {
    public record Metadata(Set<String> accountInterests, Set<String> categoryInterests) {
    }
}


