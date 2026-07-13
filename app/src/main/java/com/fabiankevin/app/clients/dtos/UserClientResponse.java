package com.fabiankevin.app.clients.dtos;

import com.fabiankevin.app.models.User;

import java.util.UUID;

public record UserClientResponse(
        UUID id,
        String firstName,
        String lastName) {

    public User toModel() {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}
