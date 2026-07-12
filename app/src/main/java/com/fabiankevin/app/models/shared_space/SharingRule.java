package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record SharingRule(
        boolean sharesOwnResources,
        Set<String> sharedResourceIds,          // specific accounts/items to share
        Set<ResourceType> visibleResourceTypes, // what resource types can see from others
        Set<UUID> visibleParticipants,          // null = all, empty = none
        Double autoApproveUnder,
        boolean requiresApproval) {
    public SharingRule {
        visibleResourceTypes = visibleResourceTypes != null ? Set.copyOf(visibleResourceTypes) : Set.of();
        sharedResourceIds = sharedResourceIds != null ? Set.copyOf(sharedResourceIds) : Set.of();
        visibleParticipants = visibleParticipants != null ? Set.copyOf(visibleParticipants) : null;
    }

    // Predefined common rules
    public static final SharingRule MUTUAL_DEFAULT = SharingRule.builder()
            .sharesOwnResources(true)
            .visibleResourceTypes(Set.of(ResourceType.values()))
            .build();

    public static final SharingRule VIEWER_DEFAULT = SharingRule.builder()
            .sharesOwnResources(false)
            .visibleResourceTypes(Set.of(ResourceType.BUDGET))
            .build();
}