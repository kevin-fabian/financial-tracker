package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.clients.dtos.CreateUserRequest;
import com.fabiankevin.app.clients.dtos.UserResponse;
import com.fabiankevin.app.services.commands.CreateUserCommand;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DefaultUserRegistrationService implements UserRegistrationService {
    private final UserCategoryProvider userCategoryProvider;
    private final UserAccountProvider userAccountProvider;
    private final UserClient userClient;

    @Transactional
    @Override
    public void register(CreateUserCommand createUserCommand) {
        CreateUserRequest request = new CreateUserRequest(
                createUserCommand.firstName(),
                createUserCommand.lastName(),
                createUserCommand.username(),
                createUserCommand.password(),
                createUserCommand.confirmPassword()
        );
        UserResponse userResponse = userClient.createUser(request);
        UUID userId = userResponse.id();

        userCategoryProvider.provide(createUserCommand.spendingInterest(), userId);
        userAccountProvider.provide(createUserCommand.accountsInterest(), userId);
    }
}
