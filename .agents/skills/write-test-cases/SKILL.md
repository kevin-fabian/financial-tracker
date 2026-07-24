---
name: write-test-cases
description: Workflow and best practices for writing clear, consistent, and maintainable Java test cases across all test layers.
---

# Test Case Best-Practice Skill

Primary goal: write test cases that are readable, behavior-focused, and consistent across the codebase for all test layers (`@WebMvcTest`, unit tests, `@DataJpaTest`, and similar).

## Workflow

1. Group all test scenarios for a method under a `@Nested` class named after the method in PascalCase (e.g. method `createUser` → `@Nested class CreateUser`).
2. Name each test method using `given<state>_then<expected_behavior>` — omit the method name, since the `@Nested` class already provides that context.
3. Arrange only the minimum data needed for the scenario.
4. Execute one clear behavior per test.
5. Assert expected outcomes with concise assertion messages.
6. For the happy-path scenario, assert all possible fields of the result.
7. Add interaction verification when behavior depends on collaborators.
8. Use `ArgumentCaptor` to verify parameters passed to collaborators for methods with no return value.
9. For each method, place the happy-path test first, followed by positive and negative/error scenarios.
10. Convert repetitive scenario matrices into parameterized tests.
11. Order `@Nested` classes by method under test.

---

## Mandatory Rules

- Always group a method's test scenarios under a `@Nested` class. Never leave multiple top-level `@Test` methods for the same method under test un-nested.
- Name the `@Nested` class in PascalCase, derived from the method under test (e.g. `organize` → `OrganizeParty`, `sendInvitation` → `SendInvitation`).
- Use test method names in `given<state>_then<expected_behavior>` format — do not repeat the method name inside the test method name.
- Use `mock()` for collaborator setup in unit tests.
- Include concise assertion messages for intent clarity.
- Use assertions to verify expected outcomes for every test.
- Use JUnit 5 assertions for simple checks (`assertEquals`, `assertNotNull`, `assertTrue`, etc.).
- Use AssertJ for complex assertions or when asserting collections (size/content/order, recursive comparison, chained extraction).
- Use `@ParameterizedTest` with `@ValueSource`, `@CsvSource`, or `@EnumSource` when inputs vary systematically.
- Use `@NullAndEmptySource` when validating null/empty string behavior.
- Order tests consistently by method under test; use `@Nested` to group multiple scenarios of the same method.
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
@ExtendWith(MockitoExtension.class)
class DefaultUserServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DefaultUserService userService = new DefaultUserService(userRepository);

    @Nested
    class CreateUser {

        // happy-path: method returns a value — assert all possible fields
        @Test
        void givenNewUser_thenReturnsUserWithAllFields() {
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

        @ParameterizedTest
        @NullAndEmptySource
        void givenNullOrEmptyNames_thenThrowsException(String name) {
            assertThrows(
                IllegalArgumentException.class,
                () -> new CreateUserCommand(name, null),
                "Creating user with null or empty name should throw IllegalArgumentException"
            );
        }
    }

    @Nested
    class FindById {

        @Test
        void givenExistingId_thenReturnsUser() {
            User user = new User("john@acme.com", "ACTIVE");
            when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

            User found = userService.findById(user.id());

            assertNotNull(found, "User should be found");
            assertEquals("john@acme.com", found.email(), "Email should match");
            assertEquals("ACTIVE", found.status(), "Status should match");
        }

        @Test
        void givenNonExistingId_thenThrows() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.findById(id));
        }
    }

    @Nested
    class Save {

        @ParameterizedTest
        @ValueSource(strings = {"ACTIVE", "INACTIVE"})
        void givenValidStatus_thenReturnsSavedUser(String status) {
            User user = new User("john@acme.com", status);
            when(userRepository.save(any())).thenReturn(user);

            User saved = userService.save(user);

            assertNotNull(saved, "Saved user should not be null");
            assertEquals(status, saved.status(), "Status should match input");
        }
    }

    @Nested
    class FindAll {

        @Test
        void givenUsersExist_thenReturnsAllUsers() {
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
    }

    @Nested
    class Delete {

        // method has no return value — use ArgumentCaptor to verify passed parameters
        @Test
        void givenExistingId_thenDelegateToRepository() {
            UUID userId = UUID.randomUUID();
            ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);

            userService.delete(userId);

            verify(userRepository).deleteById(idCaptor.capture());
            assertEquals(userId, idCaptor.getValue(), "Repository should receive the correct user id");
        }
    }
}
```

## Checklist

- every method under test has its scenarios grouped under a `@Nested` class
- `@Nested` class name is PascalCase derived from the method under test
- test names follow `given<state>_then<expected_behavior>` (no method name repetition)
- happy-path scenario asserts all possible fields
- assertions include concise messages
- JUnit assertions used for simple checks
- AssertJ used for complex checks and collection assertions
- parameterized tests used where applicable
- null/empty input covered with `@NullAndEmptySource` when relevant
- `ArgumentCaptor` used for verifying parameters on void methods
- happy-path test placed first within each `@Nested` class, followed by negative/error scenarios
- `@Nested` classes are ordered by method under test
- test scope matches the layer purpose (unit vs `@WebMvcTest` vs `@DataJpaTest`)
- no comments in test methods
