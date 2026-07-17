package com.fabiankevin.app.models.enums.shared_space;

public enum PartyMemberStatus {
    PENDING, // Invitation sent, not yet accepted
    ACTIVE,  // Currently participating
    LEFT,    // Voluntarily left
    REMOVED  // Removed by owner
}