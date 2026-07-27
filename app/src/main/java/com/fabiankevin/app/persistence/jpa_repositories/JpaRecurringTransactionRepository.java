package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.RecurringTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaRecurringTransactionRepository extends JpaRepository<RecurringTransactionEntity, UUID> {
}
