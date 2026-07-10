package com.fabiankevin.app.services;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.services.commands.CreateUserCommand;

public interface UserRegistrationService {
    User register(CreateUserCommand createUserCommand);
}
