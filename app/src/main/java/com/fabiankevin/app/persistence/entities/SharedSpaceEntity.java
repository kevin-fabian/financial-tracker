package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.SharingMode;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "shared_spaces")
@Entity
public class SharedSpaceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "space_name")
    private String spaceName;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @OneToMany(mappedBy = "sharedSpace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpaceParticipantEntity> participants;

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_mode")
    private SharingMode sharingMode;

    @OneToMany(mappedBy = "sharedSpace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SharedResourceEntity> sharedResources;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "sharesOwnResources", column = @Column(name = "default_shares_own_resources")),
            @AttributeOverride(name = "sharedResourceIds", column = @Column(name = "default_shared_resource_ids")),
            @AttributeOverride(name = "visibleResourceTypes", column = @Column(name = "default_visible_resource_types")),
            @AttributeOverride(name = "visibleParticipants", column = @Column(name = "default_visible_participants")),
            @AttributeOverride(name = "maskedFields", column = @Column(name = "default_masked_fields")),
            @AttributeOverride(name = "maxTransactionAmount", column = @Column(name = "default_max_transaction_amount")),
            @AttributeOverride(name = "autoApproveUnder", column = @Column(name = "default_auto_approve_under")),
            @AttributeOverride(name = "requiresApproval", column = @Column(name = "default_requires_approval"))
    })
    private SharingRuleEmbeddable defaultSharingRule;

    @Column(name = "active")
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public static SharedSpaceEntity from(SharedSpace space) {
        if (space == null) return null;
        SharedSpaceEntity entity = SharedSpaceEntity.builder()
                .id(space.id())
                .spaceName(space.spaceName())
                .ownerUserId(space.ownerUserId())
                .sharingMode(space.sharingMode())
                .defaultSharingRule(SharingRuleEmbeddable.from(space.defaultSharingRule()))
                .active(space.active())
                .createdAt(space.createdAt())
                .updatedAt(space.updatedAt())
                .expiresAt(space.expiresAt())
                .build();

        List<SpaceParticipantEntity> participantEntities = space.participants() != null
                ? space.participants().stream()
                        .map(SpaceParticipantEntity::from)
                        .peek(p -> p.setSharedSpace(entity))
                        .toList()
                : List.of();

        List<SharedResourceEntity> resourceEntities = space.sharedResources() != null
                ? space.sharedResources().stream()
                        .map(SharedResourceEntity::from)
                        .peek(r -> r.setSharedSpace(entity))
                        .toList()
                : List.of();

        entity.setParticipants(participantEntities);
        entity.setSharedResources(resourceEntities);
        return entity;
    }

    public SharedSpace toModel() {
        List<SpaceParticipant> participants = this.participants != null
                ? this.participants.stream().map(SpaceParticipantEntity::toModel).toList()
                : List.of();

        List<SharedResource> sharedResources = this.sharedResources != null
                ? this.sharedResources.stream().map(SharedResourceEntity::toModel).toList()
                : List.of();

        return SharedSpace.builder()
                .id(this.id)
                .spaceName(this.spaceName)
                .ownerUserId(this.ownerUserId)
                .participants(participants)
                .sharingMode(this.sharingMode)
                .sharedResources(sharedResources)
                .defaultSharingRule(Optional.ofNullable(this.defaultSharingRule).map(SharingRuleEmbeddable::toModel).orElse(null))
                .active(this.active)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .expiresAt(this.expiresAt)
                .build();
    }

}
