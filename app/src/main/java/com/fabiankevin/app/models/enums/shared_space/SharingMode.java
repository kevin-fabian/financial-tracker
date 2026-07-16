package com.fabiankevin.app.models.enums.shared_space;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SharingMode {
    MUTUAL_SHARING("Mutual Sharing", """
            This space enables bidirectional synchronization of all
            selected financial records between all invited participants
            """);

    private final String name;
    private final String description;// All participants share equally
}
