package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.sync_devices.domain.models.SyncChange;
import com.fabiankevin.app.sync_devices.domain.models.SyncChangeOperationType;
import com.fabiankevin.app.sync_devices.domain.repositories.SyncChangeRepository;
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
                .userDevice("test-device-001")
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
            assertEquals(syncChange.userDevice(), saved.userDevice(), "userDevice should match");
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
                    .userDevice("test-device-002")
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
}
