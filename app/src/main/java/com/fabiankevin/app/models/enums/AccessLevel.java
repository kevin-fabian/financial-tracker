package com.fabiankevin.app.models.enums;

import java.util.Set;

public enum AccessLevel {
    VIEW_ONLY,
    READ_WRITE; // Combines read + write (modify)
    
    // If you need hierarchical permissions, use a proper structure
    private final Set<AccessLevel> includedPermissions;
    
    AccessLevel(AccessLevel... included) {
        this.includedPermissions = Set.of(included);
    }
    
    public boolean includes(AccessLevel other) {
        return this == other || includedPermissions.contains(other);
    }
}