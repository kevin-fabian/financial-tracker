package com.fabiankevin.app.web.controllers.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Builder
public record UserCreatedEvent(
        @NotNull(message = "userId is required")
        UUID userId,
        Metadata metadata) {

    public UserCreatedEvent {
        metadata = Optional.ofNullable(metadata).orElse(Metadata.builder().build());
    }

    @Builder
    public record Metadata(Set<String> accountInterests, Set<String> categoryInterests) {
        public Metadata {
            accountInterests = Optional.ofNullable(accountInterests).orElse(Set.of());
            categoryInterests = Optional.ofNullable(categoryInterests).orElse(Set.of());
        }
    }
}


