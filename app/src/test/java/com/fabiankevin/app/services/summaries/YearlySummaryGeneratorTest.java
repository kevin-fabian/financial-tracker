package com.fabiankevin.app.services.summaries;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.queries.SummaryQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YearlySummaryGeneratorTest  {
    @Mock
    private TransactionRepository repo;

    @InjectMocks
    private YearlySummaryGenerator generator;


    @Test
    void supports_givenYearlySummaryGeneratorImplementation_shouldReturnYEARLY() {
        assertEquals(generator.supports(), SummaryType.YEARLY, "supports() should be YEARLY");
    }

    @Test
    void generate_givenValidQuery_shouldDelegateToRepositoryAndReturnPoints() {
        TransactionType transactionType = TransactionType.EXPENSE;
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        UUID userId = UUID.randomUUID();

        List<SummaryPoint> expected = List.of(new SummaryPoint("2025", BigDecimal.valueOf(100)), new SummaryPoint("2026", BigDecimal.valueOf(200)));
        when(repo.getSummaryByDateRangeAndUserIdGroupedByYear(from, to, List.of(userId), transactionType)).thenReturn(expected);

        SummaryQuery query = new SummaryQuery(SummaryType.YEARLY, from, to, List.of(userId), transactionType);

        List<SummaryPoint> result = generator.generate(query);

        assertThat(result).as("should return points delegated from repository").isEqualTo(expected);
    }
}
