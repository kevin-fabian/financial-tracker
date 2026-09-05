package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.household.AccessLevel;
import com.fabiankevin.app.models.enums.household.InvitationStatus;
import com.fabiankevin.app.models.household.Invitation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "invitee_user_id")
    private UUID inviteeUserId;

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

    private UUID householdId;

    public static InvitationEntity from(Invitation invitation) {
        if (invitation == null) return null;
        return InvitationEntity.builder()
                .id(invitation.id())
                .inviterUserId(invitation.inviterPlayerId())
                .inviteeUserId(invitation.inviteePlayerId())
                .proposedRole(invitation.proposedRole())
                .status(invitation.status())
                .createdAt(invitation.createdAt())
                .expiresAt(invitation.expiresAt())
                .householdId(invitation.partyId())
                .build();
    }

    public Invitation toModel() {
        return Invitation.builder()
                .id(this.id)
                .inviterPlayerId(this.inviterUserId)
                .inviteePlayerId(this.inviteeUserId)
                .proposedRole(this.proposedRole)
                .status(this.status)
                .createdAt(this.createdAt)
                .expiresAt(this.expiresAt)
                .partyId(this.householdId)
                .build();
    }

}
