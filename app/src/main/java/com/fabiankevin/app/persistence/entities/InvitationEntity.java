package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.Invitation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
                .status(invitation.status())
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .resultingSharedSpaceId(invitation.sharedSpaceId())
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
                .status(this.status)
                .createdAt(this.createdAt)
                .expiresAt(this.expiresAt)
                .sharedSpaceId(this.resultingSharedSpaceId)
                .build();
    }

}
