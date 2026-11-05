package com.fabiankevin.app.persistence.entities;

import com.fabiankevin.app.models.SyncDevice;
import com.fabiankevin.app.models.enums.BootstrapDirection;
import com.fabiankevin.app.models.enums.BootstrapStatus;
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
@Table(name = "sync_devices",
        indexes = {
                @Index(name = "idx_sync_devices_user_id", columnList = "user_id"),
                @Index(name = "idx_sync_devices_user_id_device", columnList = "user_id, user_device")
        })
@Entity
public class SyncDeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "user_device", nullable = false)
    private String userDevice;
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BootstrapStatus status;
    @Column(name = "direction", nullable = false)
    @Enumerated(EnumType.STRING)
    private BootstrapDirection direction;
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static SyncDeviceEntity from(SyncDevice syncDevice) {
        if (syncDevice == null) return null;
        return SyncDeviceEntity.builder()
                .id(syncDevice.id())
                .userId(syncDevice.userId())
                .userDevice(syncDevice.userDevice())
                .status(syncDevice.status())
                .direction(syncDevice.direction())
                .lastSyncedAt(syncDevice.lastSyncedAt())
                .createdAt(syncDevice.createdAt())
                .updatedAt(syncDevice.updatedAt())
                .build();
    }

    public SyncDevice toModel() {
        return SyncDevice.builder()
                .id(this.id)
                .userId(this.userId)
                .userDevice(this.userDevice)
                .status(this.status)
                .direction(this.direction)
                .lastSyncedAt(this.lastSyncedAt)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
