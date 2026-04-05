package com.fabiankevin.app.services.summaries;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.queries.SummaryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CategorySummaryGenerator implements SummaryGenerator {
    private final TransactionRepository transactionRepository;

    @Override
    public SummaryType supports() {
        return SummaryType.CATEGORY;
    }

    @Override
    public List<SummaryPoint> generate(SummaryQuery query) {
        LocalDate now = LocalDate.now();
        LocalDate from = Optional.ofNullable(query.from())
                .orElse(LocalDate.of(now.getYear(), now.getMonth(), 1));
        LocalDate to = Optional.ofNullable(query.to())
                .orElse(LocalDate.of(now.getYear(), now.getMonth(), 1).plusMonths(1).minusDays(1));

        return transactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(
                from,
                to,
                query.userIds(),
                query.transactionType()
        );
    }
}
