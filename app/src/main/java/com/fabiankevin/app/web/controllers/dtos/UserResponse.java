package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.models.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Response DTO representing user details")
public record UserResponse(
        @Schema(description = "Unique identifier of the user", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID id,
        @Schema(description = "First name of the user", example = "John")
        String firstName,
        @Schema(description = "Last name of the user", example = "Doe")
        String lastName,
        @Schema(description = "Initials of the user", example = "JD")
        String initial) {
    public static UserResponse from(final User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.id())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .initial(user.initial())
                .build();
    }
}
