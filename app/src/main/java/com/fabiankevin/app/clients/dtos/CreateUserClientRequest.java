package com.fabiankevin.app.clients.dtos;

import com.fabiankevin.app.services.commands.CreateUserCommand;
import lombok.Builder;

@Builder(toBuilder = true)
public record CreateUserClientRequest(
        String firstName,
        String lastName,
        String username,
        String password,
        String confirmPassword) {

    public static CreateUserClientRequest from(CreateUserCommand command) {
        return CreateUserClientRequest.builder()
                .firstName(command.firstName())
                .lastName(command.lastName())
                .username(command.username())
                .password(command.password())
                .confirmPassword(command.confirmPassword())
                .build();
    }
}
