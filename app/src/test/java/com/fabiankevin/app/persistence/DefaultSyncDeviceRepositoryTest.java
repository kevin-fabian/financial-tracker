package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.SyncDevice;
import com.fabiankevin.app.models.enums.BootstrapDirection;
import com.fabiankevin.app.models.enums.BootstrapStatus;
import com.fabiankevin.app.persistence.jpa_repositories.JpaSyncStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@Import(DefaultSyncDeviceRepositoryTest.ContextConfiguration.class)
@DataJpaTest
class DefaultSyncDeviceRepositoryTest {

    @MockitoSpyBean
    private JpaSyncStateRepository jpaSyncStateRepository;

    @Autowired
    private SyncDeviceRepository syncDeviceRepository;

    private SyncDevice syncDevice;

    @TestConfiguration
    static class ContextConfiguration {
        @Bean
        public SyncDeviceRepository syncDeviceRepository(JpaSyncStateRepository jpaSyncStateRepository) {
            return new DefaultSyncDeviceRepository(jpaSyncStateRepository);
        }
    }

    @BeforeEach
    void setUp() {
        syncDevice = SyncDevice.builder()
                .userId(UUID.randomUUID())
                .deviceId(UUID.randomUUID())
                .userDevice("test-device-001")
                .status(BootstrapStatus.IN_PROGRESS)
                .direction(BootstrapDirection.PULL)
                .lastSyncedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    class Save {
        @Test
        void givenValidSyncDevice_shouldPersistAndReturnWithAllFields() {
            SyncDevice saved = syncDeviceRepository.save(syncDevice);

            Optional<SyncDevice> found = syncDeviceRepository.findById(saved.id());
            SyncDevice retrieved = found.orElseThrow();

            assertNotNull(retrieved.id(), "id should be generated");
            assertEquals(syncDevice.userId(), retrieved.userId(), "userId should match");
            assertEquals(syncDevice.deviceId(), retrieved.deviceId(), "deviceId should match");
            assertEquals(syncDevice.userDevice(), retrieved.userDevice(), "userDevice should match");
            assertEquals(syncDevice.status(), retrieved.status(), "status should match");
            assertEquals(syncDevice.direction(), retrieved.direction(), "direction should match");
            assertEquals(syncDevice.lastSyncedAt(), retrieved.lastSyncedAt(), "lastSyncedAt should match");
            assertNotNull(retrieved.createdAt(), "createdAt should be set");
            assertNotNull(retrieved.updatedAt(), "updatedAt should be set");

            verify(jpaSyncStateRepository, times(1)).save(any());
            verify(jpaSyncStateRepository, times(1)).findById(saved.id());
        }

        @Test
        void givenSyncDeviceWithNullLastSyncedAt_shouldPersistAndReturnWithNullLastSyncedAt() {
            SyncDevice deviceWithoutLastSync = SyncDevice.builder()
                    .userId(UUID.randomUUID())
                    .deviceId(UUID.randomUUID())
                    .userDevice("test-device-001")
                    .status(BootstrapStatus.FAILED)
                    .direction(BootstrapDirection.PUSH)
                    .lastSyncedAt(null)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            SyncDevice saved = syncDeviceRepository.save(deviceWithoutLastSync);
            SyncDevice retrieved = syncDeviceRepository.findById(saved.id()).orElseThrow();

            assertEquals(saved.id(), retrieved.id(), "id should match");
            assertEquals(deviceWithoutLastSync.userId(), retrieved.userId(), "userId should match");
            assertEquals(deviceWithoutLastSync.deviceId(), retrieved.deviceId(), "deviceId should match");
            assertEquals(deviceWithoutLastSync.userDevice(), retrieved.userDevice(), "userDevice should match");
            assertEquals(deviceWithoutLastSync.status(), retrieved.status(), "status should match");
            assertEquals(deviceWithoutLastSync.direction(), retrieved.direction(), "direction should match");
            assertThat(retrieved.lastSyncedAt()).as("null lastSyncedAt should be persisted").isNull();
            assertNotNull(retrieved.createdAt(), "createdAt should be set");
            assertNotNull(retrieved.updatedAt(), "updatedAt should be set");

            verify(jpaSyncStateRepository, times(1)).save(any());
        }
    }

    @Nested
    class FindById {
        @Test
        void givenExistingId_shouldReturnSyncDeviceWithAllFields() {
            SyncDevice saved = syncDeviceRepository.save(syncDevice);

            Optional<SyncDevice> found = syncDeviceRepository.findById(saved.id());
            SyncDevice retrieved = found.orElseThrow();

            assertNotNull(retrieved.id(), "id should be set");
            assertEquals(syncDevice.userId(), retrieved.userId(), "userId should match");
            assertEquals(syncDevice.deviceId(), retrieved.deviceId(), "deviceId should match");
            assertEquals(syncDevice.userDevice(), retrieved.userDevice(), "userDevice should match");
            assertEquals(syncDevice.status(), retrieved.status(), "status should match");
            assertEquals(syncDevice.direction(), retrieved.direction(), "direction should match");
            assertEquals(syncDevice.lastSyncedAt(), retrieved.lastSyncedAt(), "lastSyncedAt should match");
            assertNotNull(retrieved.createdAt(), "createdAt should be set");
            assertNotNull(retrieved.updatedAt(), "updatedAt should be set");

            verify(jpaSyncStateRepository, times(1)).findById(saved.id());
        }

        @Test
        void givenNonExistingId_shouldReturnEmptyOptional() {
            Optional<SyncDevice> found = syncDeviceRepository.findById(UUID.randomUUID());

            assertThat(found).as("non-existing id should return empty optional").isEmpty();

            verify(jpaSyncStateRepository, times(1)).findById(any());
        }
    }

    @Nested
    class FindAllByUserId {
        @Test
        void givenMultipleDevicesForUser_shouldReturnAllDevices() {
            UUID userId = UUID.randomUUID();

            SyncDevice device1 = SyncDevice.builder()
                    .userId(userId)
                    .deviceId(UUID.randomUUID())
                    .userDevice("test-device-001")
                    .status(BootstrapStatus.IN_PROGRESS)
                    .direction(BootstrapDirection.PULL)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            SyncDevice device2 = SyncDevice.builder()
                    .userId(userId)
                    .deviceId(UUID.randomUUID())
                    .userDevice("test-device-001")
                    .status(BootstrapStatus.COMPLETED)
                    .direction(BootstrapDirection.PUSH)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            SyncDevice device3 = SyncDevice.builder()
                    .userId(userId)
                    .deviceId(UUID.randomUUID())
                    .userDevice("test-device-001")
                    .status(BootstrapStatus.FAILED)
                    .direction(BootstrapDirection.PULL)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            syncDeviceRepository.save(device1);
            syncDeviceRepository.save(device2);
            syncDeviceRepository.save(device3);

            List<SyncDevice> found = syncDeviceRepository.findAllByUserId(userId);

            assertThat(found).as("should return all 3 devices for user").hasSize(3);
            assertThat(found).extracting(SyncDevice::status).containsExactlyInAnyOrder(
                    BootstrapStatus.IN_PROGRESS, BootstrapStatus.COMPLETED, BootstrapStatus.FAILED);
            assertThat(found).allSatisfy(device -> assertEquals(userId, device.userId(), "userId should match"));

            verify(jpaSyncStateRepository, times(1)).findAllByUserId(userId);
        }

        @Test
        void givenNoDevicesForUser_shouldReturnEmptyList() {
            UUID userId = UUID.randomUUID();

            List<SyncDevice> found = syncDeviceRepository.findAllByUserId(userId);

            assertThat(found).as("non-existing user should return empty list").isEmpty();

            verify(jpaSyncStateRepository, times(1)).findAllByUserId(userId);
        }
    }

    @Nested
    class DeleteAllByUserId {
        @Test
        void givenExistingDevicesForUser_shouldDeleteAllAndReturnCount() {
            UUID userId = UUID.randomUUID();

            for (int i = 0; i < 3; i++) {
                SyncDevice device = SyncDevice.builder()
                        .userId(userId)
                        .deviceId(UUID.randomUUID())
                        .userDevice("test-device-001")
                        .status(BootstrapStatus.IN_PROGRESS)
                        .direction(BootstrapDirection.PULL)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                syncDeviceRepository.save(device);
            }

            long deleted = syncDeviceRepository.deleteAllByUserId(userId);

            assertThat(deleted).as("should return the number of deleted devices").isEqualTo(3);

            List<SyncDevice> remaining = syncDeviceRepository.findAllByUserId(userId);
            assertThat(remaining).as("all devices should be deleted for user").isEmpty();

            verify(jpaSyncStateRepository, times(1)).deleteAllByUserId(userId);
        }

        @Test
        void givenNonExistingUserId_shouldReturnZeroAndDeleteNothing() {
            UUID nonExistingUserId = UUID.randomUUID();

            long deleted = syncDeviceRepository.deleteAllByUserId(nonExistingUserId);

            assertThat(deleted).as("should return 0 when no devices exist for user").isZero();

            verify(jpaSyncStateRepository, times(1)).deleteAllByUserId(nonExistingUserId);
        }
    }
}
