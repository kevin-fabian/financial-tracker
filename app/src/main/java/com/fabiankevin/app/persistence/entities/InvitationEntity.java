package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.AccessLevel;
import com.fabiankevin.app.models.enums.InvitationStatus;
import com.fabiankevin.app.models.enums.SharingMode;
import com.fabiankevin.app.models.shared_space.Invitation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "invitations")
@Entity
public class InvitationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "inviter_user_id")
    private UUID inviterUserId;

    @Column(name = "invitee_email")
    private String inviteeEmail;

    @Column(name = "invitee_user_id")
    private UUID inviteeUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_sharing_mode")
    private SharingMode proposedSharingMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_role")
    private AccessLevel proposedRole;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "sharesOwnResources", column = @Column(name = "proposed_shares_own_resources")),
            @AttributeOverride(name = "sharedResourceIds", column = @Column(name = "proposed_shared_resource_ids")),
            @AttributeOverride(name = "visibleResourceTypes", column = @Column(name = "proposed_visible_resource_types")),
            @AttributeOverride(name = "visibleParticipants", column = @Column(name = "proposed_visible_participants")),
            @AttributeOverride(name = "maskedFields", column = @Column(name = "proposed_masked_fields")),
            @AttributeOverride(name = "maxTransactionAmount", column = @Column(name = "proposed_max_transaction_amount")),
            @AttributeOverride(name = "autoApproveUnder", column = @Column(name = "proposed_auto_approve_under")),
            @AttributeOverride(name = "requiresApproval", column = @Column(name = "proposed_requires_approval"))
    })
    private SharingRuleEmbeddable proposedSharingRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InvitationStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "resulting_shared_space_id")
    private UUID resultingSharedSpaceId;

    public static InvitationEntity from(Invitation invitation) {
        if (invitation == null) return null;
        return InvitationEntity.builder()
                .id(invitation.id())
                .inviterUserId(invitation.inviterUserId())
                .inviteeEmail(invitation.inviteeEmail())
                .inviteeUserId(invitation.inviteeUserId())
                .proposedSharingMode(invitation.proposedSharingMode())
                .proposedRole(invitation.proposedRole())
                .proposedSharingRule(SharingRuleEmbeddable.from(invitation.proposedSharingRule()))
                .status(invitation.status())
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .resultingSharedSpaceId(invitation.resultingSharedSpaceId())
                .build();
    }

    public Invitation toModel() {
        return Invitation.builder()
                .id(this.id)
                .inviterUserId(this.inviterUserId)
                .inviteeEmail(this.inviteeEmail)
                .inviteeUserId(this.inviteeUserId)
                .proposedSharingMode(this.proposedSharingMode)
                .proposedRole(this.proposedRole)
                .proposedSharingRule(Optional.ofNullable(this.proposedSharingRule).map(SharingRuleEmbeddable::toModel).orElse(null))
                .status(this.status)
                .createdAt(this.createdAt)
                .expiresAt(this.expiresAt)
                .resultingSharedSpaceId(this.resultingSharedSpaceId)
                .build();
    }

}
