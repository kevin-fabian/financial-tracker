package com.fabiankevin.app.web.controllers.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Response containing the result of user provisioning")
public record ProvisionUserResponse(
        @Schema(description = "User ID that was provisioned", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        String userId,
        @Schema(description = "List of provisioned account names",
                example = "[\"Checking Account\", \"Savings Account\"]")
        List<String> provisionedAccounts,
        @Schema(description = "List of provisioned category names",
                example = "[\"FOOD\", \"TRANSPORT\", \"ENTERTAINMENT\"]")
        List<String> provisionedCategories
) {
}
