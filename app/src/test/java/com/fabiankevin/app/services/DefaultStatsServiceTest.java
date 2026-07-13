package com.fabiankevin.app.services;

import com.fabiankevin.app.models.StatsSummary;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.shared_space.AccessLevel;
import com.fabiankevin.app.models.enums.shared_space.ParticipantStatus;
import com.fabiankevin.app.models.enums.shared_space.SharingMode;
import com.fabiankevin.app.models.shared_space.SharedSpace;
import com.fabiankevin.app.models.shared_space.SpaceParticipant;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultStatsServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SharedSpaceService sharedSpaceService;
    @InjectMocks
    private DefaultStatsService statsService;

    private List<SummaryPoint> summaryPoints(double income, double expenses) {
        return List.of(
                new SummaryPoint("INCOME", income),
                new SummaryPoint("EXPENSE", expenses)
        );
    }

    private SpaceParticipant participant(UUID userId) {
        return SpaceParticipant.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accessLevel(AccessLevel.READ_WRITE)
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();
    }

    private SharedSpace spaceWithParticipants(UUID ownerUserId, SpaceParticipant... participants) {
        return SharedSpace.builder()
                .id(UUID.randomUUID())
                .spaceName("Family Budget")
                .ownerUserId(ownerUserId)
                .participants(List.of(participants))
                .sharingMode(SharingMode.MUTUAL_SHARING)
                .sharedResources(List.of())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void getStatsSummary_givenValidQueryWithDates_thenShouldReturnStatsSummary() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 1, 31);

        StatsQuery query = StatsQuery.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .accountId(accountId)
                .categoryId(categoryId)
                .build();

        double currentIncome = 5000.0;
        double currentExpenses = 3000.0;
        double totalBalance = 15000.0;
        double priorBalance = 12000.0;

        when(sharedSpaceService.retrieveByUserId(userId)).thenReturn(List.of());
        when(transactionRepository.sumByTypeAndUserId(eq(Set.of(userId)), eq(fromDate), eq(toDate), eq(categoryId)))
                .thenReturn(summaryPoints(currentIncome, currentExpenses));
        when(transactionRepository.sumBalance(eq(Set.of(userId))))
                .thenReturn(totalBalance);
        when(transactionRepository.sumBalance(any(), any(), any()))
                .thenReturn(priorBalance);

        StatsSummary summary = statsService.getStatsSummary(userId, query);

        assertNotNull(summary, "StatsSummary should not be null");
        assertEquals(totalBalance, summary.totalBalance(), 0.001, "Total balance should match");
        assertEquals(currentIncome, summary.totalIncome(), 0.001, "Total income should match");
        assertEquals(currentExpenses, summary.totalExpenses(), 0.001, "Total expenses should match");
        assertEquals(25.0, summary.growthPercentage(), 0.01, "Growth percentage should reflect month-over-month balance change");

        verify(sharedSpaceService, times(1)).retrieveByUserId(userId);
        verify(transactionRepository, times(1)).sumByTypeAndUserId(any(), any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(eq(Set.of(userId)));
    }

    @Test
    void getStatsSummary_givenNullDates_thenShouldDefaultToCurrentMonth() {
        UUID userId = UUID.randomUUID();

        StatsQuery query = StatsQuery.builder()
                .build();

        double currentIncome = 2000.0;
        double currentExpenses = 1500.0;
        double totalBalance = 10000.0;

        when(sharedSpaceService.retrieveByUserId(userId)).thenReturn(List.of());
        when(transactionRepository.sumByTypeAndUserId(eq(Set.of(userId)), any(), any(), any()))
                .thenReturn(summaryPoints(currentIncome, currentExpenses));
        when(transactionRepository.sumBalance(eq(Set.of(userId))))
                .thenReturn(totalBalance);
        when(transactionRepository.sumBalance(any(), any(), any()))
                .thenReturn(0.0);

        StatsSummary summary = statsService.getStatsSummary(userId, query);

        assertNotNull(summary, "StatsSummary should not be null");
        assertEquals(totalBalance, summary.totalBalance(), 0.001, "Total balance should match");
        assertEquals(currentIncome, summary.totalIncome(), 0.001, "Total income should match");
        assertEquals(currentExpenses, summary.totalExpenses(), 0.001, "Total expenses should match");
        assertEquals(100.0, summary.growthPercentage(), 0.001, "Growth percentage should be 100.0% when prior balance is zero");

        verify(sharedSpaceService, times(1)).retrieveByUserId(userId);
        verify(transactionRepository, times(1)).sumByTypeAndUserId(any(), any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(any(), any(), any());
        verify(transactionRepository, times(1)).sumBalance(eq(Set.of(userId)));
    }

    @Nested
    class SharedUsers {

        @Test
        void givenUserBelongsToSharedSpace_thenShouldAggregateAcrossAllParticipants() {
            UUID userId = UUID.randomUUID();
            UUID partnerId = UUID.randomUUID();
            SpaceParticipant owner = participant(userId);
            SpaceParticipant partner = participant(partnerId);
            SharedSpace space = spaceWithParticipants(userId, owner, partner);
            Set<UUID> expectedUserIds = Set.of(userId, partnerId);

            StatsQuery query = StatsQuery.builder()
                    .fromDate(LocalDate.of(2026, 2, 1))
                    .toDate(LocalDate.of(2026, 2, 28))
                    .build();

            double currentIncome = 8000.0;
            double currentExpenses = 4500.0;
            double totalBalance = 25000.0;
            double priorBalance = 20000.0;

            when(sharedSpaceService.retrieveByUserId(userId)).thenReturn(List.of(space));
            when(transactionRepository.sumByTypeAndUserId(eq(expectedUserIds), any(), any(), any()))
                    .thenReturn(summaryPoints(currentIncome, currentExpenses));
            when(transactionRepository.sumBalance(eq(expectedUserIds)))
                    .thenReturn(totalBalance);
            when(transactionRepository.sumBalance(any(), any(), any()))
                    .thenReturn(priorBalance);

            StatsSummary summary = statsService.getStatsSummary(userId, query);

            assertNotNull(summary);
            assertEquals(totalBalance, summary.totalBalance(), 0.001);
            assertEquals(currentIncome, summary.totalIncome(), 0.001);
            assertEquals(currentExpenses, summary.totalExpenses(), 0.001);
            assertEquals(25.0, summary.growthPercentage(), 0.01);

            verify(sharedSpaceService).retrieveByUserId(userId);
            verify(transactionRepository).sumByTypeAndUserId(eq(expectedUserIds), any(), any(), any());
            verify(transactionRepository).sumBalance(eq(expectedUserIds));
            verify(transactionRepository).sumBalance(eq(expectedUserIds), any(), any());
        }

        @Test
        void givenUserIsParticipantButNotOwner_thenShouldStillIncludeAllSpaceParticipants() {
            UUID ownerId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID otherMemberId = UUID.randomUUID();
            SharedSpace space = spaceWithParticipants(ownerId, participant(ownerId), participant(userId), participant(otherMemberId));
            Set<UUID> expectedUserIds = Set.of(ownerId, userId, otherMemberId);

            StatsQuery query = StatsQuery.builder().build();

            when(sharedSpaceService.retrieveByUserId(userId)).thenReturn(List.of(space));
            when(transactionRepository.sumByTypeAndUserId(eq(expectedUserIds), any(), any(), any()))
                    .thenReturn(summaryPoints(6000.0, 2000.0));
            when(transactionRepository.sumBalance(eq(expectedUserIds)))
                    .thenReturn(30000.0);
            when(transactionRepository.sumBalance(any(), any(), any()))
                    .thenReturn(30000.0);

            StatsSummary summary = statsService.getStatsSummary(userId, query);

            assertNotNull(summary);
            assertEquals(0.0, summary.growthPercentage(), 0.001, "Growth should be 0 when prior and current balances are equal");

            verify(transactionRepository).sumByTypeAndUserId(eq(expectedUserIds), any(), any(), any());
            verify(transactionRepository).sumBalance(eq(expectedUserIds));
        }

        @Test
        void givenUserInMultipleSpaces_thenShouldUnionAllParticipantUserIds() {
            UUID userId = UUID.randomUUID();
            UUID partnerId = UUID.randomUUID();
            UUID roommateId = UUID.randomUUID();
            SharedSpace space1 = spaceWithParticipants(userId, participant(userId), participant(partnerId));
            SharedSpace space2 = spaceWithParticipants(roommateId, participant(roommateId), participant(userId));
            Set<UUID> expectedUserIds = Set.of(userId, partnerId, roommateId);

            StatsQuery query = StatsQuery.builder().build();

            when(sharedSpaceService.retrieveByUserId(userId)).thenReturn(List.of(space1, space2));
            when(transactionRepository.sumByTypeAndUserId(eq(expectedUserIds), any(), any(), any()))
                    .thenReturn(summaryPoints(10000.0, 7000.0));
            when(transactionRepository.sumBalance(eq(expectedUserIds)))
                    .thenReturn(40000.0);
            when(transactionRepository.sumBalance(any(), any(), any()))
                    .thenReturn(35000.0);

            StatsSummary summary = statsService.getStatsSummary(userId, query);

            assertNotNull(summary);
            assertEquals(10000.0, summary.totalIncome(), 0.001);

            verify(transactionRepository).sumByTypeAndUserId(eq(expectedUserIds), any(), any(), any());
            verify(transactionRepository).sumBalance(eq(expectedUserIds));
            verify(transactionRepository).sumBalance(eq(expectedUserIds), any(), any());
        }
    }
}
