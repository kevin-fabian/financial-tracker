package com.fabiankevin.app.models.enums;

public enum ParticipantStatus {
    PENDING, // Invitation sent, not yet accepted
    ACTIVE,  // Currently participating
    LEFT,    // Voluntarily left
    REMOVED  // Removed by owner
}