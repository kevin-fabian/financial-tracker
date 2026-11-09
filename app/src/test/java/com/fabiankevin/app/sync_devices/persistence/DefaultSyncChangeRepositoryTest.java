package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
import com.fabiankevin.app.sync_devices.domain.services.commands.SyncCursorQuery;
import com.fabiankevin.app.sync_devices.persistence.entities.SyncChangeEntity;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@Import(DefaultSyncChangeRepositoryTest.ContextConfiguration.class)
@DataJpaTest
class DefaultSyncChangeRepositoryTest {

    @MockitoSpyBean
    private JpaSyncChangeRepository jpaSyncChangeRepository;

    @Autowired
    private SyncChangeRepository syncChangeRepository;

    private SyncChange syncChange;

    @TestConfiguration
    static class ContextConfiguration {
        @Bean
        public SyncChangeRepository syncChangeRepository(JpaSyncChangeRepository jpaSyncChangeRepository) {
            return new DefaultSyncChangeRepository(jpaSyncChangeRepository);
        }
    }

    @BeforeEach
    void setUp() {
        syncChange = SyncChange.builder()
                .userId(UUID.randomUUID())
                .sequenceId(1L)
                .entityType(ResourceType.TRANSACTIONS)
                .entityId(UUID.randomUUID())
                .operation(SyncChangeOperationType.CREATE)
                .payload("{\"amount\": 100.0}")
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    class Save {

        @Test
        void givenValidSyncChange_shouldPersistAndReturnWithAllFields() {
            SyncChange saved = syncChangeRepository.save(syncChange);

            assertNotNull(saved.id(), "id should be generated");
            assertEquals(syncChange.userId(), saved.userId(), "userId should match");
            assertEquals(syncChange.sequenceId(), saved.sequenceId(), "sequenceId should match");
            assertEquals(syncChange.entityType(), saved.entityType(), "entityType should match");
            assertEquals(syncChange.entityId(), saved.entityId(), "entityId should match");
            assertEquals(syncChange.operation(), saved.operation(), "operation should match");
            assertEquals(syncChange.payload(), saved.payload(), "payload should match");
            assertNotNull(saved.createdAt(), "createdAt should be set");

            verify(jpaSyncChangeRepository, times(1)).save(any(SyncChangeEntity.class));
        }

        @Test
        void givenSyncChangeWithComplexPayload_shouldPersistAndReturnWithPayload() {
            SyncChange complexChange = SyncChange.builder()
                    .userId(UUID.randomUUID())
                    .sequenceId(2L)
                    .entityType(ResourceType.ACCOUNTS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.UPDATE)
                    .payload("{\"name\": \"Checking\", \"balance\": 5000.75, \"currency\": \"USD\"}")
                    .createdAt(Instant.now())
                    .build();

            SyncChange saved = syncChangeRepository.save(complexChange);

            assertEquals(complexChange.payload(), saved.payload(), "complex payload should match");
            assertEquals(ResourceType.ACCOUNTS, saved.entityType(), "entityType should match");
            assertEquals(SyncChangeOperationType.UPDATE, saved.operation(), "operation should match");

            verify(jpaSyncChangeRepository, times(1)).save(any(SyncChangeEntity.class));
        }
    }

    @Nested
    class PullChanges {

        @Test
        void givenMultipleCursors_shouldReturnChangesGroupedByOwnerId() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            SyncChange change1 = SyncChange.builder()
                    .userId(userId1)
                    .sequenceId(2L)
                    .entityType(ResourceType.TRANSACTIONS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            SyncChange change2 = SyncChange.builder()
                    .userId(userId1)
                    .sequenceId(3L)
                    .entityType(ResourceType.ACCOUNTS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.UPDATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            SyncChange change3 = SyncChange.builder()
                    .userId(userId2)
                    .sequenceId(1L)
                    .entityType(ResourceType.CATEGORIES)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            syncChangeRepository.save(change1);
            syncChangeRepository.save(change2);
            syncChangeRepository.save(change3);

            List<SyncCursorQuery> cursors = List.of(
                    SyncCursorQuery.builder().ownerId(userId1).lastPulledSequence(1L).build(),
                    SyncCursorQuery.builder().ownerId(userId2).lastPulledSequence(0L).build()
            );

            Map<UUID, List<SyncChange>> result = syncChangeRepository.findChangesAfterBatch(cursors);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(2, result.get(userId1).size());
            assertEquals(1, result.get(userId2).size());
        }

        @Test
        void givenCursorsWithNoMatchingChanges_shouldReturnEmptyMap() {
            UUID userId = UUID.randomUUID();

            SyncChange change = SyncChange.builder()
                    .userId(userId)
                    .sequenceId(5L)
                    .entityType(ResourceType.TRANSACTIONS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            syncChangeRepository.save(change);

            // Cursor sequence is higher than any existing change
            List<SyncCursorQuery> cursors = List.of(
                    SyncCursorQuery.builder().ownerId(userId).lastPulledSequence(10L).build()
            );

            Map<UUID, List<SyncChange>> result = syncChangeRepository.findChangesAfterBatch(cursors);

            assertNotNull(result);
            assertEquals(0, result.size());
        }

        @Test
        void givenEmptyCursors_shouldReturnEmptyMap() {
            List<SyncCursorQuery> cursors = List.of();

            Map<UUID, List<SyncChange>> result = syncChangeRepository.findChangesAfterBatch(cursors);

            assertNotNull(result);
            assertEquals(0, result.size());
        }

        @Test
        void givenCursorsWithSameUserMultipleTimes_shouldUseMaxSequence() {
            UUID userId = UUID.randomUUID();

            SyncChange change1 = SyncChange.builder()
                    .userId(userId)
                    .sequenceId(2L)
                    .entityType(ResourceType.TRANSACTIONS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            SyncChange change2 = SyncChange.builder()
                    .userId(userId)
                    .sequenceId(4L)
                    .entityType(ResourceType.ACCOUNTS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.UPDATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            syncChangeRepository.save(change1);
            syncChangeRepository.save(change2);

            // Same user appears twice — max sequence (3) is used, so only seq 4 is returned
            List<SyncCursorQuery> cursors = List.of(
                    SyncCursorQuery.builder().ownerId(userId).lastPulledSequence(1L).build(),
                    SyncCursorQuery.builder().ownerId(userId).lastPulledSequence(3L).build()
            );

            Map<UUID, List<SyncChange>> result = syncChangeRepository.findChangesAfterBatch(cursors);

            assertNotNull(result);
            assertEquals(1, result.get(userId).size());
            assertEquals(4L, result.get(userId).get(0).sequenceId());
        }
    }
}
