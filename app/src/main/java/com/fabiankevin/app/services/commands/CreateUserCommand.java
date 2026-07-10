package com.fabiankevin.app.services.commands;

import lombok.Builder;

import java.util.Set;

@Builder
public record CreateUserCommand(
        String firstName,
        String lastName,
        String username,
        String password,
        String confirmPassword,
        Set<String> categoryInterests,
        Set<String> accountInterests) {
}