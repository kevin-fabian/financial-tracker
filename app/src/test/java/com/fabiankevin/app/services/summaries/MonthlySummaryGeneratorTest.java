package com.fabiankevin.app.services.summaries;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.queries.SummaryQuery;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlySummaryGeneratorTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private MonthlySummaryGenerator generator;

    @Test
    void supports_givenMonthlySummaryGeneratorImplementation_thenReturnMonthly() {
        Assertions.assertThat(generator.supports())
                .as("supports should be MONTHLY")
                .isEqualTo(SummaryType.MONTHLY);
    }

    @Test
    void generate_givenRepositoryReturnsProjections_thenReturnMappedPoints() {
        int year = 2026;
        UUID userId = UUID.randomUUID();
        TransactionType expense = TransactionType.EXPENSE;
        SummaryPoint p1 = new SummaryPoint("3", BigDecimal.valueOf(250));
        SummaryPoint p2 = new SummaryPoint("5", BigDecimal.valueOf(8000));

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        when(transactionRepository.getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId), expense))
                .thenReturn(List.of(p1, p2));

        SummaryQuery query = SummaryQuery.builder()
                .type(SummaryType.MONTHLY)
                .transactionType(expense)
                .from(from)
                .to(to)
                .userIds(List.of(userId))
                .build();

        var result = generator.generate(query);

        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).extracting(SummaryPoint::label).containsExactlyInAnyOrder("3", "5");
        Assertions.assertThat(result).extracting(SummaryPoint::total)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(BigDecimal.valueOf(250), BigDecimal.valueOf(8000));

        verify(transactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId), expense);
    }

    @Test
    void generate_givenRepositoryReturnsEmpty_thenReturnEmptyList() {
        int year = 2025;
        TransactionType expense = TransactionType.EXPENSE;
        UUID userId = UUID.randomUUID();

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        when(transactionRepository.getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId), expense))
                .thenReturn(List.of());

        SummaryQuery query = SummaryQuery.builder()
                .type(SummaryType.MONTHLY)
                .from(from)
                .to(to)
                .userIds(List.of(userId))
                .transactionType(expense)
                .build();

        var result = generator.generate(query);

        Assertions.assertThat(result).as("result should be empty when repository returns no projections").isEmpty();
        verify(transactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId), expense);
    }
}
