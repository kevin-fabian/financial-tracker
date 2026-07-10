package com.fabiankevin.app.exceptions.users;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class UsernameAlreadyTakenException extends ApiException {
    public UsernameAlreadyTakenException(String detail) {
        super(detail, 409);
    }
}
