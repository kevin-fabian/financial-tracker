package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.entities.projections.SummaryPointProjection;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class DefaultTransactionRepository implements TransactionRepository {
    private final JpaTransactionRepository jpaTransactionRepository;

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity saved = jpaTransactionRepository.save(TransactionEntity.from(transaction));
        return saved.toModel();
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaTransactionRepository.findById(id)
                .map(TransactionEntity::toModel);
    }

    @Override
    public void deleteById(UUID id) {
        jpaTransactionRepository.deleteById(id);
    }

    @Override
    public List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByCategory(LocalDate from, LocalDate to, List<UUID> userIds) {
        return jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, userIds)
                .map(SummaryPointProjection::toModel)
                .toList();
    }

    @Override
    public List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByMonth(LocalDate from, LocalDate to, List<UUID> userIds) {
        return jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, userIds)
                .map(SummaryPointProjection::toModel)
                .toList();
    }
}
