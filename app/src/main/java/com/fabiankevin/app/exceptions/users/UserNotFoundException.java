package com.fabiankevin.app.exceptions.users;

import com.fabiankevin.app.exceptions.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String email) {
        super("User not found for email: " + email);
    }
}
