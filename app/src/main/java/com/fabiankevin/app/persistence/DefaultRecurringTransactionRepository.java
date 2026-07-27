package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.recurring_transactions.RecurringTransaction;
import com.fabiankevin.app.persistence.entities.RecurringTransactionEntity;
import com.fabiankevin.app.persistence.jpa_repositories.JpaRecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DefaultRecurringTransactionRepository implements RecurringTransactionRepository {
    private final JpaRecurringTransactionRepository jpaRecurringTransactionRepository;

    @Override
    public RecurringTransaction save(RecurringTransaction recurringTransaction) {
        return jpaRecurringTransactionRepository.save(RecurringTransactionEntity.from(recurringTransaction)).toModel();
    }
}
