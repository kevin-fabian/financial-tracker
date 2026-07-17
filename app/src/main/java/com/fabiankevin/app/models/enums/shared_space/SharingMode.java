package com.fabiankevin.app.models.enums.shared_space;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SharingMode {
    EVEN_SHARE("Even Share", """
            Even Share enables bidirectional synchronization of all
            selected financial records between all invited players
            """);

    private final String name;
    private final String description;// All partyMembers share equally
}
