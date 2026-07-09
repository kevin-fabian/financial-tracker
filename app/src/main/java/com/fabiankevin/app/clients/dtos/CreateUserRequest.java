package com.fabiankevin.app.clients.dtos;

public record CreateUserRequest(
        String firstName,
        String lastName,
        String username,
        String password,
        String confirmPassword) {
}
