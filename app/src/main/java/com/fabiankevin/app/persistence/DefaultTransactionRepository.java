package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.SummaryPoint;
import com.fabiankevin.app.models.Transaction;
import com.fabiankevin.app.models.enums.TransactionType;
import com.fabiankevin.app.persistence.entities.TransactionEntity;
import com.fabiankevin.app.persistence.entities.projections.SummaryPointProjection;
import com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository;
import com.fabiankevin.app.services.queries.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    public int deleteByIdAndUserId(UUID transactionId, UUID userId) {
        return jpaTransactionRepository.deleteByIdAndAccountUserId(transactionId, userId);
    }

    @Override
    public List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByCategory(LocalDate from, LocalDate to, Set<UUID> userIds, TransactionType type) {
        return jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, userIds, type)
                .map(SummaryPointProjection::toModel)
                .toList();
    }

    @Override
    public List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByMonth(LocalDate from, LocalDate to, Set<UUID> userIds, TransactionType type) {
        return jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByMonth(from, to, userIds, type)
                .map(SummaryPointProjection::toModel)
                .toList();
    }

    @Override
    public List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByYear(LocalDate from, LocalDate to, Set<UUID> userIds, TransactionType type) {
        return jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByYear(from, to, userIds, type)
                .map(SummaryPointProjection::toModel)
                .toList();
    }

    @Override
    public List<SummaryPoint> getSummaryByDateRangeAndUserIdGroupedByDay(LocalDate from, LocalDate to, Set<UUID> userIds, TransactionType type) {
        return jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByDay(from, to, userIds, type)
                .map(SummaryPointProjection::toModel)
                .toList();
    }

    @Override
    public List<SummaryPoint> sumByTypeAndUserId(Set<UUID> userIds, LocalDate from, LocalDate to, UUID accountId, UUID categoryId) {
        return jpaTransactionRepository.sumByTypeAndDateRange(userIds, from, to, accountId, categoryId)
                .map(SummaryPointProjection::toModel)
                .toList();
    }

    @Override
    public List<SummaryPoint> sumByTypeAndUserId(Set<UUID> userIds, LocalDate from, LocalDate to, UUID categoryId) {
        return jpaTransactionRepository.sumByTypeAndDateRangeByCategory(userIds, from, to, categoryId)
                .map(SummaryPointProjection::toModel)
                .toList();
    }

    @Override
    public double sumBalance(Set<UUID> userIds, LocalDate from, LocalDate to) {
        return jpaTransactionRepository.sumBalance(userIds, from, to);
    }

    @Override
    public double sumBalance(Set<UUID> userIds) {
        return jpaTransactionRepository.sumBalance(userIds);
    }

    @Override
    public com.fabiankevin.app.models.Page<Transaction> getTransactionsByPageAndUserId(PageQuery query, Set<UUID> userIds) {
        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort())
        );

        var page = jpaTransactionRepository.findAllByUserIdsAndType(userIds, null, pageable)
                .map(TransactionEntity::toModel);

        return new com.fabiankevin.app.models.Page<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }

    @Override
    public com.fabiankevin.app.models.Page<Transaction> getTransactionsByPageAndUserIdAndType(PageQuery query, Set<UUID> userIds, TransactionType type) {
        var pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort())
        );

        var page = jpaTransactionRepository.findAllByUserIdsAndType(userIds, type, pageable)
                .map(TransactionEntity::toModel);

        return new com.fabiankevin.app.models.Page<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }
}
