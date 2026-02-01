---
applyTo: '**/services/*ServiceTest.java'
description: 'Unit test for service implementation' 
---

## Service unit test guidelines

When writing core service test implementation, please follow these guidelines:

- Place test classes in the `services` package.
- Use static `mock()` to mock dependencies.
- Initialize the service implementation with mocked dependencies.
- Use `@ParameterizedTest` and `@ValueSource` for parameterized tests where applicable.
- Place test classes in the `services` package.
- Use `@BeforeEach` to set up mock data or configurations needed for the tests.
- The test class should be concise and focused on testing service-specific operations only.
- Cover various scenarios and all edge cases for each method in the service implementation.

Examples:

```java
package com.example.services;

import com.example.models.CodeSnippet;
import org.assertj.core.api.Assertions;

class DefaultUserServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new DefaultUserService(userRepository);
    private CreateUserCommand command;

    @BeforeEach
    void setUp() {
        command = CreateUserCommand.builder()
                .name("John Doe")
                .email("john.doe@test.com")
                .build();
    }

    @Test
    void save_givenValidCommand_thenShouldSaveUser() {
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        User user = defaultUserService.createUser(command);
        
        Assertions.assertThat(command)
                .as("Check user matches command")
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(user);
    }
}
```