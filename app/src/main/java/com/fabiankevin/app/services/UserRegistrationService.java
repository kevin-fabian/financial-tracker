package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.dtos.UserResponse;
import com.fabiankevin.app.services.commands.CreateUserCommand;

public interface UserRegistrationService {
    UserResponse register(CreateUserCommand createUserCommand);
}
