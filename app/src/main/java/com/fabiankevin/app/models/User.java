package com.fabiankevin.app.models;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record User(
        UUID id,
        String firstName,
        String lastName) {

    public String fullName() {
         return String.format("%s %s", firstName, lastName);
    }
}
