package com.fabiankevin.app.models.enums.party;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResourceType {
    TRANSACTION("Transaction", "Track and manage collective income and expenses"),
    BUDGET("Budget Goals", "Set and monitor shared financial milestones"),
    CHECKLIST("Shopping Checklist", "Collaborate on active tasks and shopping lists");

    private final String name;
    private final String description;
}