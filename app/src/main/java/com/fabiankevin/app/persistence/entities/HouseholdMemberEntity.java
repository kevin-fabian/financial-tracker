package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.enums.household.AccessLevel;
import com.fabiankevin.app.models.enums.household.HouseholdMemberStatus;
import com.fabiankevin.app.models.household.HouseholdMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@ToString(exclude = "household")
@EqualsAndHashCode(exclude = "household")
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "household_members")
@Entity
public class HouseholdMemberEntity {
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
    private HouseholdMemberStatus status;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @ManyToOne
    @JoinColumn(name = "household_id")
    private HouseholdEntity household;

    public static HouseholdMemberEntity from(HouseholdMember member) {
        if (member == null) return null;
        return HouseholdMemberEntity.builder()
                .id(member.id())
                .userId(member.userId())
                .accessLevel(member.accessLevel())
                .status(member.status())
                .joinedAt(member.joinedAt())
                .build();
    }

    public HouseholdMember toModel() {
        return HouseholdMember.builder()
                .id(this.id)
                .userId(this.userId)
                .accessLevel(this.accessLevel)
                .status(this.status)
                .joinedAt(this.joinedAt)
                .build();
    }
}
