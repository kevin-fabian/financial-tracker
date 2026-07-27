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

    public String initial() {
        String first = firstName != null && !firstName.isBlank() ? firstName.substring(0, 1) : "";
        String last = lastName != null && !lastName.isBlank() ? lastName.substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
