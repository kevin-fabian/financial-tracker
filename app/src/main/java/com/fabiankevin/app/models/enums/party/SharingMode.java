package com.fabiankevin.app.models.enums.party;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SharingMode {
    EVEN_SHARE("Even Share", """
            Splits everything equally! All party members have
            equal access to view the all party member transactions
            in real time.
            """);

    private final String name;
    private final String description;// All partyMembers share equally
}
