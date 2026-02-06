package com.fabiankevin.app.services.summaries;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.queries.SummaryQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class YearlySummaryGeneratorTest {
    @Test
    void supports_shouldReturnYEARLY() {
        TransactionRepository repo = Mockito.mock(TransactionRepository.class);
        YearlySummaryGenerator generator = new YearlySummaryGenerator(repo);

        assertThat(generator.supports()).isEqualTo(SummaryType.YEARLY);
    }

    @Test
    void generate_shouldDelegateToRepository_andReturnPoints() {
        TransactionRepository repo = Mockito.mock(TransactionRepository.class);
        YearlySummaryGenerator generator = new YearlySummaryGenerator(repo);

        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        UUID userId = UUID.randomUUID();

        List<SummaryPoint> expected = List.of(new SummaryPoint("2025", BigDecimal.valueOf(100)), new SummaryPoint("2026", BigDecimal.valueOf(200)));
        when(repo.getSummaryByDateRangeAndUserIdGroupedByYear(from, to, List.of(userId))).thenReturn(expected);

        SummaryQuery query = new SummaryQuery(com.fabiankevin.app.models.enums.SummaryType.YEARLY, from, to, List.of(userId));

        List<SummaryPoint> result = generator.generate(query);

        assertThat(result).isEqualTo(expected);
    }
}

