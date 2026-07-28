package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.DomainException;

public class InvalidDurationException extends DomainException {
    public InvalidDurationException(String message) {
        super(message);
    }
}
