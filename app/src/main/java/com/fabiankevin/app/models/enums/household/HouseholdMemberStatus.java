package com.fabiankevin.app.models.enums.household;

public enum HouseholdMemberStatus {
    PENDING, // Invitation sent, not yet accepted
    ACTIVE,  // Currently participating
    LEFT,    // Voluntarily left
    REMOVED  // Removed by owner
}