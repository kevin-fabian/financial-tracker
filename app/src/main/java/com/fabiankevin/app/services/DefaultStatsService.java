package com.fabiankevin.app.services;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import com.fabiankevin.app.web.controllers.dtos.StatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
    public StatsResponse getStats(UUID userId, StatsQuery query) {
        LocalDate now = LocalDate.now();
        LocalDate fromDate = Optional.ofNullable(query.fromDate()).orElse(now.withDayOfMonth(1));
        LocalDate toDate = Optional.ofNullable(query.toDate()).orElse(now.plusDays(1));

        // Calculate prior period (same duration, preceding the current period)
        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);
        LocalDate priorFromDate = fromDate.minusDays(daysBetween);
        LocalDate priorToDate = fromDate.minusDays(1);

        // Query current period totals (single grouped query)
        List<SummaryPoint> currentPeriod = transactionRepository.sumByTypeAndUserId(userId, fromDate, toDate, query.accountId(), query.categoryId());
        double currentIncome = sumByType(currentPeriod, TransactionType.INCOME);
        double currentExpenses = sumByType(currentPeriod, TransactionType.EXPENSE);

        // Query prior period totals
        List<SummaryPoint> priorPeriod = transactionRepository.sumByTypeAndUserId(userId, priorFromDate, priorToDate, query.accountId(), query.categoryId());
        double priorIncome = sumByType(priorPeriod, TransactionType.INCOME);
        double priorExpenses = sumByType(priorPeriod, TransactionType.EXPENSE);

        // Calculate cumulative balance (all-time, no date filter)
        double totalBalance = transactionRepository.sumBalance(userId, query.accountId());

        // Calculate growth percentage
        double currentNet = currentIncome - currentExpenses;
        double priorNet = priorIncome - priorExpenses;
        double growthPercentage = priorNet != 0.0
                ? (currentNet - priorNet) / Math.abs(priorNet) * 100.0
                : (currentNet != 0.0) ? 100.0 : 0.0;

        return StatsResponse.builder()
                .totalBalance(totalBalance)
                .totalExpenses(currentExpenses)
                .totalIncome(currentIncome)
                .growthPercentage(growthPercentage)
                .build();
    }
}
