package com.fabiankevin.app.models.shared_space;

import com.fabiankevin.app.models.enums.shared_space.ResourceType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SharingPermissionResolver {
    public boolean canViewResource(SharedSpace space, UUID viewerId, ResourceType type) {
        if (viewerId.equals(space.ownerUserId())) return true;

        SpaceParticipant viewer = space.participants().stream()
                .filter(p -> p.userId().equals(viewerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not a participant"));

        SharingRule rule = resolveRule(space, viewer);

        if (!rule.visibleResourceTypes().contains(type)) return false;
//
//        if (rule.visibleParticipants() != null && !rule.visibleParticipants().contains(resourceOwnerId)) {
//            return false;
//        }
        return true;
    }

    public SharingRule resolveRule(SharedSpace space, SpaceParticipant participant) {
        if (participant.sharingRule() != null) return participant.sharingRule();
        return switch (space.sharingMode()) {
            case MUTUAL_SHARING -> SharingRule.MUTUAL_DEFAULT;
        };
    }
}