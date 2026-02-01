package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Transaction;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(UUID id);
    void deleteById(UUID id);
}
