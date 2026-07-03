---
name: write-test-cases
description: Workflow and best practices for writing clear, consistent, and maintainable Java test cases across all test layers.
---

# Test Case Best-Practice Skill

Primary goal: write test cases that are readable, behavior-focused, and consistent across the codebase for all test layers (`@WebMvcTest`, unit tests, `@DataJpaTest`, and similar).

## Workflow

1. Name each test using `methodName_<given>_<then>`.
2. Arrange only the minimum data needed for the scenario.
3. Execute one clear behavior per test.
4. Assert expected outcomes with concise assertion messages.
5. Add interaction verification when behavior depends on collaborators.
6. Convert repetitive scenario matrices into parameterized tests.
7. Order tests by method under test; use `@Nested` to group multiple scenarios of the same method.

---

## Mandatory Rules

- Use test method names in `methodName_<given>_<then>` format.
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
class UserServiceTest {

    @Test
    void findById_existingId_returnsUser() {
        User user = new User("john@acme.com");

        assertEquals("john@acme.com", user.email(), "Email should match the created user");
        assertNotNull(user, "User should not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "INACTIVE"})
    void save_validStatus_savesSuccessfully(String status) {
        User user = new User("john@acme.com", status);

        assertEquals(status, user.status(), "Status should match input");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void save_nullOrEmptyName_throwsException(String invalidName) {
        assertThrows(
            IllegalArgumentException.class,
            () -> new User(invalidName),
            "Creating user with null or empty name should throw IllegalArgumentException"
        );
    }

    @Test
    void findAll_existingUsers_returnsExpectedCollection() {
        List<User> users = List.of(
            new User("john@acme.com", "ACTIVE"),
            new User("jane@acme.com", "INACTIVE")
        );

        assertThat(users)
            .as("Returned users should contain expected emails and size")
            .hasSize(2)
            .extracting(User::email)
            .containsExactlyInAnyOrder("john@acme.com", "jane@acme.com");
    }
}
```

## Checklist

- test names follow `methodName_<given>_<then>`
- assertions include concise messages
- JUnit assertions used for simple checks
- AssertJ used for complex checks and collection assertions
- parameterized tests used where applicable
- null/empty input covered with `@NullAndEmptySource` when relevant
- tests are ordered by method under test
- `@Nested` is used when a method has multiple scenarios
- test scope matches the layer purpose (unit vs `@WebMvcTest` vs `@DataJpaTest`)
- no comments in test methods
