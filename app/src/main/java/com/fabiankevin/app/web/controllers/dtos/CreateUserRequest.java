package com.fabiankevin.app.web.controllers.dtos;

import com.fabiankevin.app.services.commands.CreateUserCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Set;

import static com.fabiankevin.app.models.constants.AppValidationRules.*;

@Builder(toBuilder = true)
public record CreateUserRequest(
        @Schema(description = "User's first name", example = "John", minLength = 2, maxLength = 50)
        @Pattern(regexp = NAME_REGEX, message = "First name can only contain alphabetic characters, spaces, hyphens, and apostrophes")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters if provided")
        @NotBlank(message = "First name is required.")
        String firstName,

        @Schema(description = "User's last name", example = "Doe", minLength = 2, maxLength = 50)
        @Pattern(regexp = NAME_REGEX, message = "Last name can only contain alphabetic characters, spaces, hyphens, and apostrophes")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters if provided")
        @NotBlank(message = "Last name is required.")
        String lastName,

        @Schema(description = "Username for authentication", example = "john.doe", minLength = 3, maxLength = 50)
        @Pattern(regexp = USERNAME_REGEX, message = "Email must be from a valid domain")
        @NotBlank(message = "Username is required")
        @Size(min = USERNAME_MIN_LENGTH, max = USERNAME_MAX_LENGTH, message = "Username must be between 3 and 50 characters")
        String username,

        @Schema(description = "User's password for authentication", example = "secretP@ssw0rd", minLength = 8, maxLength = 100)
        @Pattern(regexp = PASSWORD_REGEX, message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
        @NotBlank(message = "Password is required")
        @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH, message = "Password must be between 8 and 100 characters")
        String password,

        @Schema(description = "User's password confirmation for authentication", example = "secretP@ssw0rd", minLength = 8, maxLength = 100)
        @NotBlank(message = "Confirm password is required")
        String confirmPassword,

        @Schema(description = "User's spending & saving interests for initial categories", examples = "groceries, bills")
        Set<String> categoryInterests,

        @Schema(description = "User's account interests for initial accounts", examples = "bank, credit_card")
        Set<String> accountInterests) {

    public CreateUserCommand toCommand() {
        return new CreateUserCommand(username, password, confirmPassword, lastName, firstName, categoryInterests, accountInterests);
    }
}
