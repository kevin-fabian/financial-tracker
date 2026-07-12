package com.fabiankevin.app.models.enums;

public enum SharingMode {
    OWNER_PROVIDES,      // Only the space owner shares resources
    MUTUAL_SHARING,      // All participants share equally
    CUSTOM_SHARING       // Per-participant sharing rules (SharingRule)
}
