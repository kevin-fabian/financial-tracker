package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.services.commands.CreateUserCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DefaultUserRegistrationService implements UserRegistrationService {
    private final UserCategoryProvider userCategoryProvider;
    private final UserAccountProvider userAccountProvider;
    private final UserClient userClient;

    @Override
    public void register(CreateUserCommand createUserCommand) {

    }
}
