package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(UUID id);
    void deleteById(UUID id);
    List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByCategory(LocalDate from, LocalDate to, List<UUID> userIds);
    List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByMonth(LocalDate from, LocalDate to, List<UUID> userIds);
    List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByYear(LocalDate from, LocalDate to, List<UUID> userIds);
    List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByDay(LocalDate from, LocalDate to, List<UUID> userIds);
}
