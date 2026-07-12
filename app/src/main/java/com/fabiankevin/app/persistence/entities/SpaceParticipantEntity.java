package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Optional;
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

    @Column(name = "invited_by_user_id")
    private UUID invitedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ParticipantStatus status;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "sharesOwnResources", column = @Column(name = "shares_own_resources")),
            @AttributeOverride(name = "sharedResourceIds", column = @Column(name = "shared_resource_ids")),
            @AttributeOverride(name = "visibleResourceTypes", column = @Column(name = "visible_resource_types")),
            @AttributeOverride(name = "visibleParticipants", column = @Column(name = "visible_participants")),
            @AttributeOverride(name = "maskedFields", column = @Column(name = "masked_fields")),
            @AttributeOverride(name = "maxTransactionAmount", column = @Column(name = "max_transaction_amount")),
            @AttributeOverride(name = "autoApproveUnder", column = @Column(name = "auto_approve_under")),
            @AttributeOverride(name = "requiresApproval", column = @Column(name = "requires_approval"))
    })
    private SharingRuleEmbeddable sharingRule;

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
                .sharingRule(SharingRuleEmbeddable.from(participant.sharingRule()))
                .build();
    }

    public SpaceParticipant toModel() {
        return SpaceParticipant.builder()
                .id(this.id)
                .userId(this.userId)
                .accessLevel(this.accessLevel)
                .status(this.status)
                .joinedAt(this.joinedAt)
                .sharingRule(Optional.ofNullable(this.sharingRule).map(SharingRuleEmbeddable::toModel).orElse(null))
                .build();
    }
}
