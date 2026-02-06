package com.fabiankevin.app.services.summaries;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.services.queries.SummaryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class DailySummaryGenerator implements SummaryGenerator {
    private final TransactionRepository transactionRepository;

    @Override
    public SummaryType supports() {
        return SummaryType.DAILY;
    }

    @Override
    public List<SummaryPoint> generate(SummaryQuery query) {
        return transactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(
                query.from(),
                query.to(),
                query.userIds()
        );
    }
}

