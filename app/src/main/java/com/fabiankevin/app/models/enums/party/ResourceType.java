package com.fabiankevin.app.models.enums.party;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResourceType {
    TRANSACTION("Party Loot(Transactions)", """
        All shared loot! Every piece of income and expense is visible to the party, keeping your total balance and monthly stats perfectly mirrored.
        """),

    BUDGET("Bounty Quests(Budget Goals)", """
        Shared hunting limits! All party members can view and track active category limits to ensure the group stays under budget.
        """),

    CHECKLIST("Quest Log(Shopping Lists)", """
        Private by default. The creator of the gathering list controls exactly who gets to see or help complete these specific tasks.
        """);

    private final String name;
    private final String description;
}