package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.DomainException;

public class InvalidNotesException extends DomainException {
    public InvalidNotesException(String message) {
        super(message);
    }
}
