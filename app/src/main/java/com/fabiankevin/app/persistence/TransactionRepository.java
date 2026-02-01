package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.Transaction;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
}
