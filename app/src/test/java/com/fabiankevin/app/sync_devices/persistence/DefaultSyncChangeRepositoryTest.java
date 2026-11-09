package com.fabiankevin.app.sync_devices.persistence;

import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.enums.ResourceType;
import com.fabiankevin.app.services.queries.PageQuery;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    class FindChangesAfterBatchCursor {

        @Test
        void givenUserIdWithChanges_shouldReturnChangesOrderedBySequenceId() {
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
                    .sequenceId(3L)
                    .entityType(ResourceType.ACCOUNTS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.UPDATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            SyncChange change3 = SyncChange.builder()
                    .userId(userId)
                    .sequenceId(5L)
                    .entityType(ResourceType.CATEGORIES)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.CREATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            syncChangeRepository.save(change1);
            syncChangeRepository.save(change2);
            syncChangeRepository.save(change3);

            PageQuery pageQuery = PageQuery.builder().page(0).size(100).build();
            Page<SyncChange> result = syncChangeRepository.findChangesAfterBatch(userId, 1L, pageQuery);

            assertNotNull(result);
            assertEquals(3, result.content().size());
            assertThat(result.content())
                    .extracting(SyncChange::sequenceId)
                    .containsExactly(2L, 3L, 5L);
        }

        @Test
        void givenUserIdWithNoChangesAfterSequence_shouldReturnEmptyList() {
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

            PageQuery pageQuery = PageQuery.builder().page(0).size(100).build();
            Page<SyncChange> result = syncChangeRepository.findChangesAfterBatch(userId, 10L, pageQuery);

            assertNotNull(result);
            assertEquals(0, result.content().size());
        }

        @Test
        void givenLimit_shouldRespectPageSize() {
            UUID userId = UUID.randomUUID();

            for (long i = 1; i <= 5; i++) {
                SyncChange change = SyncChange.builder()
                        .userId(userId)
                        .sequenceId(i)
                        .entityType(ResourceType.TRANSACTIONS)
                        .entityId(UUID.randomUUID())
                        .operation(SyncChangeOperationType.CREATE)
                        .payload("{}")
                        .createdAt(Instant.now())
                        .build();
                syncChangeRepository.save(change);
            }

            PageQuery pageQuery = PageQuery.builder().page(0).size(3).build();
            Page<SyncChange> result = syncChangeRepository.findChangesAfterBatch(userId, 0L, pageQuery);

            assertNotNull(result);
            assertEquals(3, result.content().size());
            assertThat(result.content())
                    .extracting(SyncChange::sequenceId)
                    .containsExactly(1L, 2L, 3L);
        }

        @Test
        void givenUserIdWithNoChangesAtAll_shouldReturnEmptyList() {
            UUID userId = UUID.randomUUID();

            PageQuery pageQuery = PageQuery.builder().page(0).size(100).build();
            Page<SyncChange> result = syncChangeRepository.findChangesAfterBatch(userId, 0L, pageQuery);

            assertNotNull(result);
            assertEquals(0, result.content().size());
        }

        @Test
        void givenDifferentUsers_shouldReturnOnlyRequestedUserChanges() {
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
                    .userId(userId2)
                    .sequenceId(3L)
                    .entityType(ResourceType.ACCOUNTS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.UPDATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            syncChangeRepository.save(change1);
            syncChangeRepository.save(change2);

            PageQuery pageQuery = PageQuery.builder().page(0).size(100).build();
            Page<SyncChange> result = syncChangeRepository.findChangesAfterBatch(userId1, 0L, pageQuery);

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertThat(result.content()).allMatch(change -> change.userId().equals(userId1));
        }
    }

    @Nested
    class FindChangesAfterBatchPageQuery {

        @Test
        void givenUserIdWithMoreChangesThanPageSize_shouldReturnPaginatedResultWithHasMore() {
            UUID userId = UUID.randomUUID();

            for (long i = 1; i <= 15; i++) {
                SyncChange change = SyncChange.builder()
                        .userId(userId)
                        .sequenceId(i)
                        .entityType(ResourceType.TRANSACTIONS)
                        .entityId(UUID.randomUUID())
                        .operation(SyncChangeOperationType.CREATE)
                        .payload("{}")
                        .createdAt(Instant.now())
                        .build();
                syncChangeRepository.save(change);
            }

            PageQuery pageQuery = PageQuery.withDefaults();

            Page<SyncChange> result = syncChangeRepository.findChangesAfterBatch(userId, 0L, pageQuery);

            assertNotNull(result);
            assertEquals(10, result.content().size(), "should return page size of 10");
            assertEquals(15, result.totalElements());
            assertEquals(2, result.totalPages());
            assertThat(result.content())
                    .extracting(SyncChange::sequenceId)
                    .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        }

        @Test
        void givenUserIdWithFewerChangesThanPageSize_shouldReturnAllChangesAndMarkAsLast() {
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
                    .sequenceId(5L)
                    .entityType(ResourceType.ACCOUNTS)
                    .entityId(UUID.randomUUID())
                    .operation(SyncChangeOperationType.UPDATE)
                    .payload("{}")
                    .createdAt(Instant.now())
                    .build();

            syncChangeRepository.save(change1);
            syncChangeRepository.save(change2);

            PageQuery pageQuery = PageQuery.withDefaults();

            Page<SyncChange> result = syncChangeRepository.findChangesAfterBatch(userId, 0L, pageQuery);

            assertNotNull(result);
            assertEquals(2, result.content().size());
            assertThat(result.content())
                    .extracting(SyncChange::sequenceId)
                    .containsExactly(2L, 5L);
            assertEquals(2, result.totalElements());
            assertEquals(1, result.totalPages());
            assertThat(result.last()).isTrue();
        }
    }
}
