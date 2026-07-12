package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.shared_space.SharingRule;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SharingRuleEmbeddable {
    @Column(name = "shares_own_resources")
    private boolean sharesOwnResources;

    @Column(name = "shared_resource_ids")
    private String sharedResourceIds;

    @Column(name = "visible_resource_types")
    private String visibleResourceTypes;

    @Column(name = "visible_participants")
    private String visibleParticipants;

    @Column(name = "auto_approve_under")
    private Double autoApproveUnder;

    @Column(name = "requires_approval")
    private boolean requiresApproval;

    public static SharingRuleEmbeddable from(SharingRule rule) {
        if (rule == null) return null;
        return SharingRuleEmbeddable.builder()
                .sharesOwnResources(rule.sharesOwnResources())
                .sharedResourceIds(joinStrings(rule.sharedResourceIds()))
                .visibleResourceTypes(joinEnums(rule.visibleResourceTypes()))
                .visibleParticipants(joinUuids(rule.visibleParticipants()))
                .autoApproveUnder(rule.autoApproveUnder())
                .requiresApproval(rule.requiresApproval())
                .build();
    }

    public SharingRule toModel() {
        return SharingRule.builder()
                .sharesOwnResources(this.sharesOwnResources)
                .sharedResourceIds(splitStrings(this.sharedResourceIds))
                .visibleResourceTypes(splitResourceTypes(this.visibleResourceTypes))
                .visibleParticipants(splitUuids(this.visibleParticipants))
                .autoApproveUnder(this.autoApproveUnder)
                .requiresApproval(this.requiresApproval)
                .build();
    }

    private static String joinStrings(Set<String> values) {
        if (values == null || values.isEmpty()) return null;
        return String.join(",", values);
    }

    private static Set<String> splitStrings(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String joinUuids(Set<UUID> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    private static Set<UUID> splitUuids(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String joinEnums(Set<ResourceType> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private static Set<ResourceType> splitResourceTypes(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(","))
                .map(ResourceType::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
