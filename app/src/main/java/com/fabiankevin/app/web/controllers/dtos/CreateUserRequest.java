package com.fabiankevin.app.web.controllers.dtos;

import java.util.Set;

public record CreateUserRequest(
        String firstName,
        String lastName,
        String username,
        String password,
        String confirmPassword,
        Set<String> spendingInterest,
        Set<String> accountsInterest) {
}
