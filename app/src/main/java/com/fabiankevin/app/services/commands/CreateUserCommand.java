package com.fabiankevin.app.services.commands;

import java.util.Set;

public record CreateUserCommand(
        String firstName,
        String lastName,
        String username,
        String password,
        String confirmPassword,
        Set<String> categoryInterests,
        Set<String> accountInterests) {
}