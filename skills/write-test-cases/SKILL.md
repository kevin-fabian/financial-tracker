---
name: write-test-cases
description: Workflow and best practices for writing clear, consistent, and maintainable Java test cases across all test layers.
---

# Test Case Best-Practice Skill

Primary goal: write test cases that are readable, behavior-focused, and consistent across the codebase for all test layers (`@WebMvcTest`, unit tests, `@DataJpaTest`, and similar).

## Workflow

1. Name each test using `methodName_given<state>_then<expected_behavior>`.
2. Arrange only the minimum data needed for the scenario.
3. Execute one clear behavior per test.
4. Assert expected outcomes with concise assertion messages.
5. For the happy-path scenario, assert all possible fields of the result.
6. Add interaction verification when behavior depends on collaborators.
7. Use `ArgumentCaptor` to verify parameters passed to collaborators for methods with no return value.
8. For each method, place the happy-path test first, followed by positive and negative/error scenarios.
9. Convert repetitive scenario matrices into parameterized tests.
10. Order tests by method under test; use `@Nested` to group multiple scenarios of the same method.

---

## Mandatory Rules

- Use test method names in `methodName_given<state>_then<expected_behavior>` format.
- Use `mock()` for collaborator setup in unit tests.
- Include concise assertion messages for intent clarity.
- Use assertions to verify expected outcomes for every test.
- Use JUnit 5 assertions for simple checks (`assertEquals`, `assertNotNull`, `assertTrue`, etc.).
- Use AssertJ for complex assertions or when asserting collections (size/content/order, recursive comparison, chained extraction).
- Use `@ParameterizedTest` with `@ValueSource`, `@CsvSource`, or `@EnumSource` when inputs vary systematically.
- Use `@NullAndEmptySource` when validating null/empty string behavior.
- Order tests consistently by method name/method under test.
- Use `@Nested` classes when one method has multiple test scenarios.
- Keep clear blank-line separation between setup, execution, and assertion blocks.
- Strictly no comments inside test cases.

---

## Layer Applicability

- Unit tests: isolate business logic and mock external collaborators.
- `@WebMvcTest`: focus on controller request/response behavior, validation, status codes, and JSON contracts.
- `@DataJpaTest`: focus on repository persistence behavior, query correctness, and entity mapping.
- For any layer, keep each test focused on one behavior and assert both success and failure scenarios where relevant.

---

## Practical Examples

```java
class DefaultUserServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DefaultUserService userService = new DefaultUserService(userRepository);

    // example 1: method returns a value — assert all possible fields
    @Test
    void createUser_givenNewUser_thenDelegateExpectedParameters(){
        CreateUserCommand command = CreateUserCommand.builder()
                .firstName("john")
                .lastName("doe")
                .build();

        when(userRepository.save(any())).thenAnswer(i -> {
            User user = i.getArgument(0);
            return user.toBuilder().id(UUID.randomUUID())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        User createdUser = userService.createUser(command);

        assertNotNull(createdUser.id(), "id should be generated");
        assertEquals("john", createdUser.firstName(), "firstName should match command");
        assertEquals("doe", createdUser.lastName(), "lastName should match command");
        assertNotNull(createdUser.createdAt(), "createdAt should be generated");
        assertNotNull(createdUser.updatedAt(), "updatedAt should be generated");
    }

    @Test
    void findById_givenExistingId_thenReturnsUser() {
        User user = new User("john@acme.com", "ACTIVE");
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        User found = userService.findById(user.id());

        assertNotNull(found, "User should be found");
        assertEquals("john@acme.com", found.email(), "Email should match");
        assertEquals("ACTIVE", found.status(), "Status should match");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "INACTIVE"})
    void save_givenValidStatus_thenReturnsSavedUser(String status) {
        User user = new User("john@acme.com", status);
        when(userRepository.save(any())).thenReturn(user);

        User saved = userService.save(user);

        assertNotNull(saved, "Saved user should not be null");
        assertEquals(status, saved.status(), "Status should match input");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void updateStatus_givenNullOrEmptyStatus_thenThrowsException(String invalidStatus) {
        assertThrows(
            IllegalArgumentException.class,
            () -> new User("john@acme.com", invalidStatus),
            "Creating user with null or empty status should throw IllegalArgumentException"
        );
    }

    @Test
    void findAll_givenUsersExist_thenReturnsAllUsers() {
        List<User> users = List.of(
            new User("john@acme.com", "ACTIVE"),
            new User("jane@acme.com", "INACTIVE")
        );
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();

        assertThat(result)
            .as("Returned users should contain expected emails and size")
            .hasSize(2)
            .extracting(User::email)
            .containsExactlyInAnyOrder("john@acme.com", "jane@acme.com");
    }

    // example 2: method has no return value — use ArgumentCaptor to verify passed parameters
    @Test
    void delete_givenExistingId_thenDelegateToRepository() {
        UUID userId = UUID.randomUUID();
        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);

        userService.delete(userId);

        verify(userRepository).deleteById(idCaptor.capture());
        assertEquals(userId, idCaptor.getValue(), "Repository should receive the correct user id");
    }
}
```

## Checklist

- test names follow `methodName_given<state>_then<expected_behavior>`
- happy-path scenario asserts all possible fields
- assertions include concise messages
- JUnit assertions used for simple checks
- AssertJ used for complex checks and collection assertions
- parameterized tests used where applicable
- null/empty input covered with `@NullAndEmptySource` when relevant
- `ArgumentCaptor` used for verifying parameters on void methods
- happy-path test placed first, followed by negative/error scenarios
- tests are ordered by method under test
- `@Nested` is used when a method has multiple scenarios
- test scope matches the layer purpose (unit vs `@WebMvcTest` vs `@DataJpaTest`)
- no comments in test methods
