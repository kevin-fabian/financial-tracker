package com.fabiankevin.app.services;

import com.fabiankevin.app.services.commands.CreateUserCommand;

public interface UserRegistrationService {
    void register(CreateUserCommand createUserCommand);
}
