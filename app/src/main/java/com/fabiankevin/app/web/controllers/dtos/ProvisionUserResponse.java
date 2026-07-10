package com.fabiankevin.app.web.controllers.dtos;

import lombok.Builder;

import java.util.List;

@Builder
public record ProvisionUserResponse(
        String userId,
        List<String> provisionedAccounts,
        List<String> provisionedCategories) {
}
