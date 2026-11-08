package com.fabiankevin.app.sync_devices.persistence.entities;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sync_changes",
        indexes = {
                @Index(name = "idx_sync_changes_user_id", columnList = "user_id"),
                @Index(name = "idx_sync_changes_user_id_device", columnList = "user_id, user_device"),
                @Index(name = "idx_sync_changes_user_id_sequence", columnList = "user_id, sequence_id")
        })
@Entity
public class SyncChangeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "user_device", nullable = false)
    private String userDevice;
    @Column(name = "sequence_id", nullable = false)
    private Long sequenceId;
    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceType entityType;
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    @Column(name = "operation", nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncChangeOperationType operation;
    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static SyncChangeEntity from(SyncChange syncChange) {
        if (syncChange == null) return null;
        return SyncChangeEntity.builder()
                .id(syncChange.id())
                .userId(syncChange.userId())
                .userDevice(syncChange.userDevice())
                .sequenceId(syncChange.sequenceId())
                .entityType(syncChange.entityType())
                .entityId(syncChange.entityId())
                .operation(syncChange.operation())
                .payload(syncChange.payload())
                .createdAt(syncChange.createdAt())
                .build();
    }

    public SyncChange toModel() {
        return SyncChange.builder()
                .id(this.id)
                .userId(this.userId)
                .userDevice(this.userDevice)
                .sequenceId(this.sequenceId)
                .entityType(this.entityType)
                .entityId(this.entityId)
                .operation(this.operation)
                .payload(this.payload)
                .createdAt(this.createdAt)
                .build();
    }
}
