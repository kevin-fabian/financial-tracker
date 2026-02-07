package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.services.queries.PageQuery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(UUID id);
    void deleteById(UUID id);
    // Delete a transaction by id only when it belongs to the specified userId.
    // Returns the number of rows deleted (0 if none). Implementation should be idempotent (no exception if not found).
    int deleteByIdAndUserId(UUID transactionId, UUID userId);
    List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByCategory(LocalDate from, LocalDate to, List<UUID> userIds, TransactionType type);
    List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByMonth(LocalDate from, LocalDate to, List<UUID> userIds, TransactionType type);
    List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByYear(LocalDate from, LocalDate to, List<UUID> userIds, TransactionType type);
    List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByDay(LocalDate from, LocalDate to, List<UUID> userIds, TransactionType type);
    Page<Transaction> getTransactionsByPageAndUserId(PageQuery query, UUID userId);
}
