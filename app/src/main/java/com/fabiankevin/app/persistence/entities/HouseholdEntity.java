package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.household.Household;
import com.fabiankevin.app.models.household.HouseholdMember;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
@Table(name = "households")
@Entity
public class HouseholdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "leader_id")
    private UUID leaderId;

    @OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HouseholdMemberEntity> members = new HashSet<>();

    @Column(name = "active")
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static HouseholdEntity from(Household household) {
        if (household == null) return null;
        HouseholdEntity entity = HouseholdEntity.builder()
                .id(household.id())
                .name(household.name())
                .leaderId(household.leaderId())
                .active(household.active())
                .members(new HashSet<>())
                .createdAt(household.createdAt())
                .updatedAt(household.updatedAt())
                .build();

        for (HouseholdMember participant : household.members()) {
            entity.addMember(HouseholdMemberEntity.from(participant));
        }

        return entity;
    }

    public void addMember(HouseholdMemberEntity member) {
        if (member == null) return;
        member.setHousehold(this);
        this.members.add(member);
    }

    public Household toModel() {
        List<HouseholdMember> members = this.members != null
                ? this.members.stream().map(HouseholdMemberEntity::toModel).toList()
                : List.of();

        return Household.builder()
                .id(this.id)
                .name(this.name)
                .leaderId(this.leaderId)
                .members(members)
                .active(this.active)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

}
