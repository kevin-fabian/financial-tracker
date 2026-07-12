package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.SharedResource;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @OneToMany(mappedBy = "sharedSpace", cascade = CascadeType.ALL, orphanRemoval = true, fetch =  FetchType.EAGER)
    private Set<SpaceParticipantEntity> participants = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_mode")
    private SharingMode sharingMode;

    @OneToMany(mappedBy = "sharedSpace", cascade = CascadeType.ALL, orphanRemoval = true, fetch =  FetchType.EAGER)
    private Set<SharedResourceEntity> sharedResources = new HashSet<>();

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
                .active(space.active())
                .participants(new HashSet<>())
                .sharedResources(new HashSet<>())
                .createdAt(space.createdAt())
                .updatedAt(space.updatedAt())
                .build();

        for (SpaceParticipant participant : space.participants()) {
            entity.addParticipant(SpaceParticipantEntity.from(participant));
        }

        for (SharedResource sharedResource : space.sharedResources()) {
            entity.addResource(SharedResourceEntity.from(sharedResource));
        }

        return entity;
    }

    public void addParticipant(SpaceParticipantEntity participant) {
        if (participant == null) return;
        participant.setSharedSpace(this);
        this.participants.add(participant);
    }

    public void addResource(SharedResourceEntity resource) {
        if (resource == null) return;
        resource.setSharedSpace(this);
        this.sharedResources.add(resource);
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
                .active(this.active)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

}
