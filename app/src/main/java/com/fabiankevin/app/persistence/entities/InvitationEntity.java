package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.InvitationStatus;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.party.Invitation;
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

    @Column(name = "inviter_player_id")
    private UUID inviterPlayerId;

    @Column(name = "invitee_player_id")
    private UUID inviteePlayerId;

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

    private UUID partyId;

    public static InvitationEntity from(Invitation invitation) {
        if (invitation == null) return null;
        return InvitationEntity.builder()
                .id(invitation.id())
                .inviterPlayerId(invitation.inviterPlayerId())
                .inviteePlayerId(invitation.inviteePlayerId())
                .proposedSharingMode(invitation.proposedSharingMode())
                .proposedRole(invitation.proposedRole())
                .status(invitation.status())
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .partyId(invitation.sharedSpaceId())
                .build();
    }

    public Invitation toModel() {
        return Invitation.builder()
                .id(this.id)
                .inviterPlayerId(this.inviterPlayerId)
                .inviteePlayerId(this.inviteePlayerId)
                .proposedSharingMode(this.proposedSharingMode)
                .proposedRole(this.proposedRole)
                .status(this.status)
                .createdAt(this.createdAt)
                .expiresAt(this.expiresAt)
                .sharedSpaceId(this.partyId)
                .build();
    }

}
