package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.party.SharingMode;
import com.fabiankevin.app.models.party.Party;
import com.fabiankevin.app.models.party.PartyMember;
import com.fabiankevin.app.models.party.SharedItem;
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
@Table(name = "parties")
@Entity
public class PartyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "party_leader_id")
    private UUID partyLeaderId;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true, fetch =  FetchType.EAGER)
    private Set<PartyMemberEntity> partyMembers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_mode")
    private SharingMode sharingMode;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true, fetch =  FetchType.EAGER)
    private Set<SharedItemEntity> sharedItems = new HashSet<>();

    @Column(name = "active")
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public static PartyEntity from(Party space) {
        if (space == null) return null;
        PartyEntity entity = PartyEntity.builder()
                .id(space.id())
                .name(space.name())
                .partyLeaderId(space.partyLeaderId())
                .sharingMode(space.sharingMode())
                .active(space.active())
                .partyMembers(new HashSet<>())
                .sharedItems(new HashSet<>())
                .createdAt(space.createdAt())
                .updatedAt(space.updatedAt())
                .build();

        for (PartyMember participant : space.partyMembers()) {
            entity.addParticipant(PartyMemberEntity.from(participant));
        }

        for (SharedItem sharedItem : space.sharedItems()) {
            entity.addResource(SharedItemEntity.from(sharedItem));
        }

        return entity;
    }

    public void addParticipant(PartyMemberEntity participant) {
        if (participant == null) return;
        participant.setParty(this);
        this.partyMembers.add(participant);
    }

    public void addResource(SharedItemEntity resource) {
        if (resource == null) return;
        resource.setParty(this);
        this.sharedItems.add(resource);
    }

    public Party toModel() {
        List<PartyMember> participants = this.partyMembers != null
                ? this.partyMembers.stream().map(PartyMemberEntity::toModel).toList()
                : List.of();

        List<SharedItem> sharedItems = this.sharedItems != null
                ? this.sharedItems.stream().map(SharedItemEntity::toModel).toList()
                : List.of();

        return Party.builder()
                .id(this.id)
                .name(this.name)
                .partyLeaderId(this.partyLeaderId)
                .partyMembers(participants)
                .sharingMode(this.sharingMode)
                .sharedItems(sharedItems)
                .active(this.active)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

}
