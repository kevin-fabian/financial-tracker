package com.fabiankevin.app.web.controllers.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Builder
@Schema(description = "Event payload for user creation with metadata about interests")
public record UserCreatedEvent(
        @NotNull(message = "id is required")
        @Schema(description = "Unique identifier of the user", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "Metadata containing account and category interests")
        Metadata metadata) {

    public UserCreatedEvent {
        metadata = Optional.ofNullable(metadata).orElse(Metadata.builder().build());
    }

    @Builder
    @Schema(description = "Metadata containing account and category interests")
    public record Metadata(
            @Schema(description = "Set of account type interests",
                    example = "[\"E_WALLET\", \"BANK_ACCOUNT\"]")
            Set<String> accountInterests,
            @Schema(description = "Set of category type interests",
                    example = "[\"FOOD\", \"TRANSPORT\", \"ENTERTAINMENT\"]")
            Set<String> categoryInterests) {
        public Metadata {
            accountInterests = Optional.ofNullable(accountInterests).orElse(Set.of());
            categoryInterests = Optional.ofNullable(categoryInterests).orElse(Set.of());
        }
    }
}


