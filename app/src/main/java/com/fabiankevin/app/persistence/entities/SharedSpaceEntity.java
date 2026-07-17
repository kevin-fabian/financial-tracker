package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Party;
import com.fabiankevin.app.models.shared_space.PartyMember;
import com.fabiankevin.app.models.shared_space.SharedItem;
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

    public static SharedSpaceEntity from(Party space) {
        if (space == null) return null;
        SharedSpaceEntity entity = SharedSpaceEntity.builder()
                .id(space.id())
                .spaceName(space.name())
                .ownerUserId(space.partyLeaderId())
                .sharingMode(space.sharingMode())
                .active(space.active())
                .participants(new HashSet<>())
                .sharedResources(new HashSet<>())
                .createdAt(space.createdAt())
                .updatedAt(space.updatedAt())
                .build();

        for (PartyMember participant : space.partyMembers()) {
            entity.addParticipant(SpaceParticipantEntity.from(participant));
        }

        for (SharedItem sharedItem : space.sharedItems()) {
            entity.addResource(SharedResourceEntity.from(sharedItem));
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

    public Party toModel() {
        List<PartyMember> participants = this.participants != null
                ? this.participants.stream().map(SpaceParticipantEntity::toModel).toList()
                : List.of();

        List<SharedItem> sharedItems = this.sharedResources != null
                ? this.sharedResources.stream().map(SharedResourceEntity::toModel).toList()
                : List.of();

        return Party.builder()
                .id(this.id)
                .name(this.spaceName)
                .partyLeaderId(this.ownerUserId)
                .partyMembers(participants)
                .sharingMode(this.sharingMode)
                .sharedItems(sharedItems)
                .active(this.active)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

}
