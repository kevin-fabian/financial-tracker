package com.fabiankevin.app.models.enums.party;

public enum PartyMemberStatus {
    PENDING, // Invitation sent, not yet accepted
    ACTIVE,  // Currently participating
    LEFT,    // Voluntarily left
    REMOVED  // Removed by owner
}