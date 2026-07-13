package com.fabiankevin.app.services;

import com.fabiankevin.app.models.StatsSummary;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DefaultStatsService implements StatsService {
    private final TransactionRepository transactionRepository;

    private static double sumByType(List<SummaryPoint> points, TransactionType type) {
        return points.stream()
                .filter(p -> p.label().equals(type.name()))
                .mapToDouble(SummaryPoint::total)
                .sum();
    }

    @Override
    public StatsSummary getStatsSummary(UUID userId, StatsQuery query) {
        LocalDate now = LocalDate.now();
        LocalDate fromDate = Optional.ofNullable(query.fromDate()).orElse(now.withDayOfMonth(1));
        LocalDate toDate = Optional.ofNullable(query.toDate()).orElse(now);

        // Query current period totals (single grouped query)
        List<SummaryPoint> currentPeriod = transactionRepository.sumByTypeAndUserId(Set.of(userId), fromDate, toDate, query.categoryId());
        double currentIncome = sumByType(currentPeriod, TransactionType.INCOME);
        double currentExpenses = sumByType(currentPeriod, TransactionType.EXPENSE);

        // Calculate cumulative balance (all-time, across all accounts)
        double totalBalance = transactionRepository.sumBalance(Set.of(userId));
        double totalBalanceLastMonthWithSameDate = transactionRepository.sumBalance(Set.of(userId),
                now.minusMonths(1).withDayOfMonth(1),
                now.minusMonths(1)
        );

        double growthPercentage = totalBalanceLastMonthWithSameDate != 0.0
                ? (totalBalance - totalBalanceLastMonthWithSameDate) / Math.abs(totalBalanceLastMonthWithSameDate) * 100.0
                : (totalBalance != 0.0) ? 100.0 : 0.0;

        return StatsSummary.builder()
                .totalBalance(totalBalance)
                .totalExpenses(currentExpenses)
                .totalIncome(currentIncome)
                .growthPercentage(growthPercentage)
                .build();
    }
}
