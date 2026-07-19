package com.fabiankevin.app.models.enums.party;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SharingMode {
    EVEN_SHARE("Even Share", """
            Splits everything equally! All party members share
            full access to view, co-manage, and update your
            selected shared elements in real time.
            """);

    private final String name;
    private final String description;// All partyMembers share equally
}
