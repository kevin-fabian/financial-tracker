package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response for a created user")
public record CreateUserResponse(
        @Schema(description = "User identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0852")
        UUID id,

        @Schema(description = "User's first name", example = "John")
        String firstName,

        @Schema(description = "User's last name", example = "Doe")
        String lastName
) {
    public static CreateUserResponse from(User userResponse) {
        return CreateUserResponse.builder()
                .id(userResponse.id())
                .firstName(userResponse.firstName())
                .lastName(userResponse.lastName())
                .build();
    }
}
