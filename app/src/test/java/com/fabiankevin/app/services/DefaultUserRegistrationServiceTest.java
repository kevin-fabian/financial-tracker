package com.fabiankevin.app.services;

import com.fabiankevin.app.clients.UserClient;
import com.fabiankevin.app.clients.dtos.CreateUserRequest;
import com.fabiankevin.app.clients.dtos.UserResponse;
import com.fabiankevin.app.services.commands.CreateUserCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultUserRegistrationServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private UserCategoryProvider userCategoryProvider;

    @Mock
    private UserAccountProvider userAccountProvider;

    @InjectMocks
    private DefaultUserRegistrationService service;

    @Test
    void register_givenValidCommand_thenShouldCreateUserAndInitializeCategoriesAndAccounts() {
        UUID userId = UUID.randomUUID();
        String firstName = "John";
        String lastName = "Doe";
        String username = "johndoe";
        String password = "secret";
        String confirmPassword = "secret";
        Set<String> spendingInterest = Set.of("groceries", "bills");
        Set<String> accountsInterest = Set.of("gcash", "bank");

        CreateUserCommand command = new CreateUserCommand(
                firstName, lastName, username, password, confirmPassword,
                spendingInterest, accountsInterest
        );
        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .firstName(firstName)
                .lastName(lastName)
                .build();

        when(userClient.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);
        doNothing().when(userCategoryProvider).provide(spendingInterest, userId);
        doNothing().when(userAccountProvider).provide(accountsInterest, userId);

        service.register(command);

        ArgumentCaptor<CreateUserRequest> requestCaptor = ArgumentCaptor.forClass(CreateUserRequest.class);
        verify(userClient).createUser(requestCaptor.capture());
        CreateUserRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.firstName()).isEqualTo(firstName);
        assertThat(capturedRequest.lastName()).isEqualTo(lastName);
        assertThat(capturedRequest.username()).isEqualTo(username);
        assertThat(capturedRequest.password()).isEqualTo(password);
        assertThat(capturedRequest.confirmPassword()).isEqualTo(confirmPassword);

        verify(userCategoryProvider).provide(spendingInterest, userId);
        verify(userAccountProvider).provide(accountsInterest, userId);
    }

    @Test
    void register_givenNullUserClient_throwsException() {
        CreateUserCommand command = new CreateUserCommand(
                "John", "Doe", "johndoe", "secret", "secret",
                Set.of("groceries"), Set.of("gcash")
        );
        service = new DefaultUserRegistrationService(
                userCategoryProvider, userAccountProvider, null
        );

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void register_givenUserClientReturnsNull_throwsException() {
        CreateUserCommand command = new CreateUserCommand(
                "John", "Doe", "johndoe", "secret", "secret",
                Set.of("groceries"), Set.of("gcash")
        );

        when(userClient.createUser(any(CreateUserRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void register_givenProviderThrows_throwsExceptionPropagation() {
        UUID userId = UUID.randomUUID();
        CreateUserCommand command = new CreateUserCommand(
                "John", "Doe", "johndoe", "secret", "secret",
                Set.of("groceries"), Set.of("gcash")
        );
        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .build();

        when(userClient.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);
        doThrow(new RuntimeException("Provider error")).when(userCategoryProvider).provide(any(), any());

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Provider error");

        verify(userAccountProvider, never()).provide(any(), any());
    }
}
