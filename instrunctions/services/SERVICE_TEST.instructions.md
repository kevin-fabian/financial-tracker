---
applyTo: '**/*ServiceTest.java'
description: 'Unit test guidelines for service implementations in this repository' 
---

## Writing Service Implementation Tests Best Practices

### Do

- Initialize the service implementation with mocked dependencies. e.g., `private final UserRepository userRepository = mock(UserRepository.class); private final UserService userService = new DefaultUserService(userRepository);`
- The test class should be concise and focused on testing service-specific operations only.


--- 

## Example Service Implementation Test

```java
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

    @Test
    void retrieveById_givenExistingId_thenShouldReturnUser() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .name("John Doe")
                .email("john.doe@test.com")
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = defaultUserService.retrieveById(id);

        Assertions.assertThat(result)
                .as("Check user matches expected")
                .usingRecursiveComparison()
                .isEqualTo(user);
    }
}
```