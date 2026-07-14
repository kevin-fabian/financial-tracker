package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@ToString(exclude = "sharedSpace")
@EqualsAndHashCode(exclude = "sharedSpace")
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "space_participants")
@Entity
public class SpaceParticipantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level")
    private AccessLevel accessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ParticipantStatus status;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @ManyToOne
    @JoinColumn(name = "shared_space_id")
    private SharedSpaceEntity sharedSpace;

    public static SpaceParticipantEntity from(SpaceParticipant participant) {
        if (participant == null) return null;
        return SpaceParticipantEntity.builder()
                .id(participant.id())
                .userId(participant.userId())
                .accessLevel(participant.accessLevel())
                .status(participant.status())
                .joinedAt(participant.joinedAt())
                .build();
    }

    public SpaceParticipant toModel() {
        return SpaceParticipant.builder()
                .id(this.id)
                .userId(this.userId)
                .accessLevel(this.accessLevel)
                .status(this.status)
                .joinedAt(this.joinedAt)
                .build();
    }
}
