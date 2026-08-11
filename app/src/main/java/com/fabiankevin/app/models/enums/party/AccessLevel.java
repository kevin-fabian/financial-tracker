package com.fabiankevin.app.models.enums.party;

import lombok.Getter;

@Getter
public enum AccessLevel {
    VIEW_ONLY("View Only", """
        Look, but don't touch! Party members can watch the live data and stats, but cannot add, edit, or delete any records.
        """);

    private final String name;
    private final String description;

    AccessLevel(String name, String description) {
        this.name = name;
        this.description = description;
    }
}