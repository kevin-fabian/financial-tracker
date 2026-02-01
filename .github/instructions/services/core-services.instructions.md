---
applyTo: '**/services/*Service.java'
description: 'Core service interfaces and implementations' 
---

## Service Interface and implementation requirements

When writing core service interface and implementation, please follow these guidelines:

- Place service interfaces and implementations in the `services` package.
- Use `@Transactional` annotation where necessary to manage transactions.
- Use Lombok `@Slf4j` and `@RequiredArgsConstructor` for logging and dependency injections.
- Each file should contain a single interface or class.
- Name the interface or class ending with `Service`.
- Every method return type should be a domain model or a collection of domain models. e.g., `User`, `List<User>`, `Optional<User>`, etc.
- Use command objects (e.g., `CreateUserCommand`, `PatchUserCommand`) for method parameters instead of domain models.
- Use `@Service` annotation for implementation classes.
- The interface or class should be concise and focused on service-specific operations only.
- Use custom exceptions where necessary to handle error scenarios (e.g., UserNotFoundException).

Examples:

```java
package com.example.services.commands;

import com.example.models.User;
import com.example.services.commands.CreateUserCommand;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Builder(toBuilder = true)
public record CreateUserCommand(String name, String email) {
}
```

```java
package com.example.services;

import com.example.models.User;
import Com.example.services.commands.CreateUserCommand;

public interface UserService {
    User createUser(CreateUserCommand command);
    User retrieveById(UUID id);
}
```

```java
package com.example.services;

import com.example.models.User;
import com.example.persistence.repositories.UserRepository;
import com.example.services.commands.CreateUserCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.attribute.UserPrincipalNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultUserService implements UserService {
    private final UserRepository userRepository;

    @Override
    public User createUser(CreateUserCommand command) {
        return userRepository.save(User.builder()
                .name(command.name())
                .email(command.email())
                .build());
    }

    @Override
    public User retrieveById(UUID id) {
        return userRepository.findById(id)
                .orThrow(UserNotFoundException::new);
    }
}
```
