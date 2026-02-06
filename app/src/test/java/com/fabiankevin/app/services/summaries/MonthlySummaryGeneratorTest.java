package com.fabiankevin.app.services.summaries;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.persistence.entities.projections.SummaryPointProjection;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.queries.SummaryQuery;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Streamable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlySummaryGeneratorTest {

    @Mock
    private JpaTransactionRepository jpaTransactionRepository;

    @InjectMocks
    private MonthlySummaryGenerator generator;

    @Test
    void supports_givenNothing_thenReturnMonthly() {
        var result = generator.supports();

        Assertions.assertThat(result).isEqualTo(SummaryType.MONTHLY);
    }

    @Test
    void generate_givenRepositoryReturnsProjections_thenReturnMappedPoints() {
        int year = 2026;
        UUID userId = UUID.randomUUID();
        SummaryPointProjection p1 = new SummaryPointProjection("3", BigDecimal.valueOf(250));
        SummaryPointProjection p2 = new SummaryPointProjection("5", BigDecimal.valueOf(8000));

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        when(jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId)))
                .thenReturn(Streamable.of(p1, p2));

        SummaryQuery query = SummaryQuery.builder()
                .type(SummaryType.MONTHLY)
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

        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId));
    }

    @Test
    void generate_givenRepositoryReturnsEmpty_thenReturnEmptyList() {
        int year = 2025;
        UUID userId = UUID.randomUUID();

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);

        when(jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId)))
                .thenReturn(Streamable.empty());

        SummaryQuery query = SummaryQuery.builder()
                .type(SummaryType.MONTHLY)
                .from(from)
                .to(to)
                .userIds(List.of(userId))
                .build();

        var result = generator.generate(query);

        Assertions.assertThat(result).as("result should be empty when repository returns no projections").isEmpty();
        verify(jpaTransactionRepository, times(1)).getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, List.of(userId));
    }
}

