package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.DomainException;

public class DailyTransactionLimitExceededException extends DomainException {
    public DailyTransactionLimitExceededException(long limit) {
        super("Daily transaction limit of %d exceeded".formatted(limit));
    }
}
