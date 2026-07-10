package com.fabiankevin.app.clients;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.services.commands.CreateUserCommand;

public interface UserClient {
    User createUser(CreateUserCommand command);
}
