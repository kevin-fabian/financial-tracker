package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Transaction;
import jakarta.transaction.Transactional;


public class DefaultTransactionRepository implements TransactionRepository {
    @Override
    @Transactional
    public Transaction save(Transaction transaction) {

        return null;
    }
}
