package com.fabiankevin.app.accounts.web.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "Request DTO for creating multiple accounts in a batch")
public record CreateBatchAccountsRequest(
        @NotEmpty(message = "At least one account request is required")
        @Size(max = 100, message = "Batch size must not exceed 100 accounts")
        @Schema(description = "List of account creation requests")
        List<CreateAccountRequest> accounts
) {
}
