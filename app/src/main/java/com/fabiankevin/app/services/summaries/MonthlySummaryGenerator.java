package com.fabiankevin.app.services.summaries;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.queries.SummaryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class MonthlySummaryGenerator implements SummaryGenerator {
    private final TransactionRepository transactionRepository;

    @Override
    public SummaryType supports() {
        return SummaryType.MONTHLY;
    }

    @Override
    public List<SummaryPoint> generate(SummaryQuery query) {
        LocalDate now = LocalDate.now();
        LocalDate from = Optional.ofNullable(query.from())
                .orElse(LocalDate.of(now.getYear(), 1, 1));
        LocalDate to = Optional.ofNullable(query.to())
                .orElse(LocalDate.of(now.getYear(), 12, 31));
        return transactionRepository.getSummaryByDateRangeAndUserIdGroupedByMonth(
                        from,
                        to,
                        query.userIds(),
                        query.transactionType()
                ).stream()
                .map(point -> new SummaryPoint(Month.of(Integer.parseInt(point.label())).name(), point.total()))
                .toList();
    }
}
