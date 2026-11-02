package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.SyncState;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.models.enums.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
@Table(name = "sync_states",
        indexes = {
                @Index(name = "idx_sync_states_user_id", columnList = "user_id")
        })
@Entity
public class SyncStateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "user_device")
    private String userDevice;
    @Column(name = "resource_type", nullable = false)
    private String resourceType;
    @Column(name = "sync_status", nullable = false)
    private String syncStatus;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static SyncStateEntity from(SyncState syncState) {
        if (syncState == null) return null;
        return SyncStateEntity.builder()
                .id(syncState.id())
                .userId(syncState.userId())
                .userDevice(syncState.userDevice())
                .resourceType(syncState.resourceType().name())
                .syncStatus(syncState.syncStatus().name())
                .createdAt(syncState.createdAt())
                .updatedAt(syncState.updatedAt())
                .build();
    }

    public SyncState toModel() {
        return SyncState.builder()
                .id(this.id)
                .userId(this.userId)
                .userDevice(this.userDevice)
                .resourceType(Optional.ofNullable(this.resourceType).map(ResourceType::valueOf).orElse(null))
                .syncStatus(Optional.ofNullable(this.syncStatus).map(SyncStatus::valueOf).orElse(null))
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
