package com.fabiankevin.app.services;

import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.TransactionRepository;
import com.fabiankevin.app.web.controllers.dtos.StatsQuery;
import com.fabiankevin.app.web.controllers.dtos.StatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DefaultStatsService implements StatsService {

    private record DateRange(LocalDate from, LocalDate to) {
    }

    private final TransactionRepository transactionRepository;

    @Override
    public StatsResponse getStats(UUID userId, StatsQuery query) {
        LocalDate now = LocalDate.now();
        LocalDate fromDate = Optional.ofNullable(query.fromDate()).orElse(now.withDayOfMonth(1));
        LocalDate toDate = Optional.ofNullable(query.toDate()).orElse(now.plusDays(1));

        // Calculate prior period (same duration, preceding the current period)
        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);
        LocalDate priorFromDate = fromDate.minusDays(daysBetween);
        LocalDate priorToDate = fromDate.minusDays(1);

        // Query current period totals
        double currentIncome = transactionRepository.sumByTypeAndUserId(
                userId, TransactionType.INCOME, fromDate, toDate, query.accountId(), query.categoryId());
        double currentExpenses = transactionRepository.sumByTypeAndUserId(
                userId, TransactionType.EXPENSE, fromDate, toDate, query.accountId(), query.categoryId());

        // Query prior period totals
        double priorIncome = transactionRepository.sumByTypeAndUserId(
                userId, TransactionType.INCOME, priorFromDate, priorToDate, query.accountId(), query.categoryId());
        double priorExpenses = transactionRepository.sumByTypeAndUserId(
                userId, TransactionType.EXPENSE, priorFromDate, priorToDate, query.accountId(), query.categoryId());

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
