package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.services.commands.CreateUserCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DefaultUserRegistrationService implements UserRegistrationService {
    private final UserCategoryProvider userCategoryProvider;
    private final UserAccountProvider userAccountProvider;
    private final UserClient userClient;

    @Transactional
    @Override
    public User register(CreateUserCommand createUserCommand) {
        User userResponse = userClient.createUser(createUserCommand);

        userCategoryProvider.provide(createUserCommand.categoryInterests(), userResponse.id());
        userAccountProvider.provide(createUserCommand.accountInterests(), userResponse.id());

        return userResponse;
    }
}
