package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.party.AccessLevel;
import com.fabiankevin.app.models.enums.party.PartyMemberStatus;
import com.fabiankevin.app.models.party.PartyMember;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@ToString(exclude = "party")
@EqualsAndHashCode(exclude = "party")
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "party_members")
@Entity
public class PartyMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "player_id")
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level")
    private AccessLevel accessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PartyMemberStatus status;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @ManyToOne
    @JoinColumn(name = "party_id")
    private PartyEntity party;

    public static PartyMemberEntity from(PartyMember participant) {
        if (participant == null) return null;
        return PartyMemberEntity.builder()
                .id(participant.id())
                .playerId(participant.playerId())
                .accessLevel(participant.accessLevel())
                .status(participant.status())
                .joinedAt(participant.joinedAt())
                .build();
    }

    public PartyMember toModel() {
        return PartyMember.builder()
                .id(this.id)
                .playerId(this.playerId)
                .accessLevel(this.accessLevel)
                .status(this.status)
                .joinedAt(this.joinedAt)
                .build();
    }
}
