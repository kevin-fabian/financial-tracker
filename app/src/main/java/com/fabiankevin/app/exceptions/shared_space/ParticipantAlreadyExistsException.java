package com.fabiankevin.app.exceptions.shared_space;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public class ParticipantAlreadyExistsException extends ApiException {
    public ParticipantAlreadyExistsException() {
        super("User is already a participant in this space", 409);
    }
}
