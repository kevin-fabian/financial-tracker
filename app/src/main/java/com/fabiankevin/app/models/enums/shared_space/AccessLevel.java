package com.fabiankevin.app.models.enums.shared_space;

import lombok.Getter;

@Getter
public enum AccessLevel {
    VIEW_ONLY("View Only", ""),
    READ_WRITE("Read & Write", "");

    private final String name;
    private final String description;

    AccessLevel(String name, String description) {
        this.name = name;
        this.description = description;
    }
}