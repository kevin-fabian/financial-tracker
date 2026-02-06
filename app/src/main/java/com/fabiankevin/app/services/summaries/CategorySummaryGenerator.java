package com.fabiankevin.app.services.summaries;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.SummaryType;
import com.fabiankevin.app.persistence.entities.projections.SummaryPointProjection;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.queries.SummaryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CategorySummaryGenerator implements SummaryGenerator {
    private final JpaTransactionRepository jpaTransactionRepository;

    @Override
    public SummaryType supports() {
        return SummaryType.CATEGORY;
    }

    @Override
    public List<SummaryPoint> generate(SummaryQuery query) {
        return jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(
                        query.from(),
                        query.to(),
                        query.userIds()
                ).map(SummaryPointProjection::toModel)
                .toList();
    }
}
