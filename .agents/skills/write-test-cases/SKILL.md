---
name: write-test-cases
description: Workflow and best practices for writing clear, consistent, and maintainable Java test cases across all test layers. The happy-path test MUST assert every field of the result — this is non-negotiable for controller, service, and repository layers.
---

# Test Case Best-Practice Skill

Primary goal: write test cases that are readable, behavior-focused, and consistent across the codebase for all test layers (`@WebMvcTest`, `@SpringBootTest`, unit tests, `@DataJpaTest`, and similar).

## Workflow

1. Group all test scenarios for a method under a `@Nested` class named after the method in PascalCase (e.g. method `createUser` → `@Nested class CreateUser`).
2. Name each test method using `given<state>_then<expected_behavior>` — omit the method name, since the `@Nested` class already provides that context.
3. Arrange only the minimum data needed for the scenario.
4. Execute one clear behavior per test.
5. Assert expected outcomes with concise assertion messages.
6. **Happy-path rule (all layers):** the first test case for every method MUST be the happy path and MUST assert **every field** of the returned result — no exceptions. Subsequent scenarios assert only what the scenario verifies.
7. Add interaction verification when behavior depends on collaborators.
8. Use `ArgumentCaptor` to verify parameters passed to collaborators for methods with no return value.
9. For each method, place the happy-path test first, followed by positive and negative/error scenarios.
10. Convert repetitive scenario matrices into parameterized tests.
11. Order `@Nested` classes by method under test.

---

## Mandatory Rules

- **Happy-path field coverage (all layers):** the first test case for every method MUST be the happy path and MUST assert **every field** of the returned result — no exceptions. This applies to controller (`@WebMvcTest`, `@SpringBootTest`), service (unit), and repository (`@DataJpaTest`) layers. Use `jsonPath` for controller tests, direct field access for service tests, and entity field assertions for repository tests. Subsequent (non-happy-path) scenarios assert only what the scenario verifies — scenario-based assertions, not full-field enumeration.
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
- `@WebMvcTest` / `@SpringBootTest`: focus on controller request/response behavior, validation, status codes, and JSON contracts.
- `@DataJpaTest`: focus on repository persistence behavior, query correctness, and entity mapping.
- For any layer, keep each test focused on one behavior and assert both success and failure scenarios where relevant.

## Happy-Path vs Scenario-Based Assertions

The first test case for every method MUST be the happy path and MUST assert **every field** of the returned result. Subsequent scenarios assert only what the scenario verifies. This applies to all layers.

### Controller Layer (`@SpringBootTest` / `@WebMvcTest`)

Use `jsonPath` to assert every field of the response DTO in the happy path:

```java
@Nested
class GetRecurringTransactions {

    // HAPPY PATH: assert EVERY field of RecurringSummaryResponse
    @Test
    void givenRecurringTransactionsExist_thenReturnsListOfSummaries() throws Exception {
        // ... arrange ...

        mockMvc.perform(get("/api/recurring-transactions")
                        .with(jwt()...))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].description").value("Monthly subscription"))
                .andExpect(jsonPath("$[0].amount").value(15.99))
                .andExpect(jsonPath("$[0].variableAmount").value(false))
                .andExpect(jsonPath("$[0].categoryId").value(category.id().toString()))
                .andExpect(jsonPath("$[0].categoryName").value("GROCERIES"))
                .andExpect(jsonPath("$[0].accountId").value(account.id().toString()))
                .andExpect(jsonPath("$[0].accountName").value("Cash Wallet"))
                .andExpect(jsonPath("$[0].dayOfMonth").value(15))
                .andExpect(jsonPath("$[0].nextOccurrenceDate").exists())
                .andExpect(jsonPath("$[0].endDate").exists())
                .andExpect(jsonPath("$[0].remainingDays").isNumber())
                .andExpect(jsonPath("$[0].transactionStatus").value("UPCOMING"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].initial").value("JD"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists());
    }

    // SCENARIO-BASED: assert only what the scenario verifies
    @Test
    void givenNoRecurringTransactions_thenReturnsEmptyList() throws Exception {
        // ... arrange ...

        mockMvc.perform(get("/api/recurring-transactions")
                        .with(jwt()...))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
```

```java
@Nested
class Create {

    // HAPPY PATH: assert EVERY field
    @Test
    void givenValidRequest_thenReturnsCreatedWithSummary() throws Exception {
        // ... arrange ...

        mockMvc.perform(post("/api/recurring-transactions")...)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value("Monthly subscription"))
                // ... assert every field of RecurringSummaryResponse ...
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    // SCENARIO-BASED: status code + minimal field checks
    @Test
    void givenNoJwt_thenReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/recurring-transactions")...)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenNonExistentAccount_thenReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/recurring-transactions")...)
                .andExpect(status().isNotFound());
    }
}
```

### Service Layer (Unit Test with `@ExtendWith(MockitoExtension.class)`)

Assert every field of the returned domain model in the happy path:

```java
@Nested
class Create {

    // HAPPY PATH: assert EVERY field of RecurringTransactionSummary
    @Test
    void givenValidCommand_thenReturnsSummaryWithAllFields() {
        // ... arrange ...

        RecurringTransactionSummary summary = service.create(command);

        assertNotNull(summary, "summary should not be null");
        assertNotNull(summary.id(), "id should be generated");
        assertEquals("Monthly subscription", summary.description(), "description should match command");
        assertEquals(15.99, summary.amount(), "amount should match command");
        assertFalse(summary.variableAmount(), "variableAmount should be false");
        assertNotNull(summary.category(), "category should not be null");
        assertEquals(category.id(), summary.category().id(), "category should match");
        assertNotNull(summary.account(), "account should not be null");
        assertEquals(account.id(), summary.account().id(), "account should match");
        assertEquals(15, summary.dayOfMonth(), "dayOfMonth should match command");
        assertNotNull(summary.nextOccurrenceDate(), "nextOccurrenceDate should not be null");
        assertNotNull(summary.endDate(), "endDate should not be null");
        assertTrue(summary.remainingDays() > 0, "remainingDays should be positive");
        assertEquals(TransactionStatus.UPCOMING, summary.transactionStatus(), "status should be UPCOMING");
        assertEquals(RecurringTransactionStatus.ACTIVE, summary.status(), "recurring status should be ACTIVE");
        assertNotNull(summary.user(), "user should not be null");
        assertEquals("Kevin", summary.user().firstName(), "user firstName should match");
        assertEquals("Fabian", summary.user().lastName(), "user lastName should match");
        assertNotNull(summary.createdAt(), "createdAt should not be null");
        assertNotNull(summary.updatedAt(), "updatedAt should not be null");
    }

    // SCENARIO-BASED: assert only what the scenario verifies
    @Test
    void givenNonExistentAccount_thenThrowsAccountNotFoundException() {
        when(accountRepository.findById(account.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command))
                .as("should throw AccountNotFoundException when account does not exist")
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void givenVariableAmountFlagWithNonZeroAmount_thenThrowsInvalidAmountException() {
        CreateRecurringTransactionCommand invalidCommand = command.toBuilder()
                .variableAmount(true).amount(15.99).build();

        assertThatThrownBy(() -> service.create(invalidCommand))
                .as("should throw InvalidAmountException when variableAmount is true and amount is non-zero")
                .isInstanceOf(InvalidAmountException.class);
    }
}
```

### Repository Layer (`@DataJpaTest`)

Assert every persisted field in the happy path:

```java
@Nested
class Save {

    // HAPPY PATH: assert EVERY field of the persisted entity
    @Test
    void givenValidRecurringTransaction_persistsAndRetrievesAllFields() {
        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);

        assertNotNull(saved.id(), "recurring transaction id should have been generated");
        assertEquals(recurringTransaction.description(), saved.description(), "description should match");
        assertEquals(recurringTransaction.amount(), saved.amount(), "amount should match");
        assertEquals(recurringTransaction.dayOfMonth(), saved.dayOfMonth(), "dayOfMonth should match");
        assertEquals(recurringTransaction.status(), saved.status(), "status should match");
        assertEquals(recurringTransaction.category().id(), saved.category().id(), "category should match");
        assertEquals(recurringTransaction.account().id(), saved.account().id(), "account should match");
        assertEquals(recurringTransaction.nextOccurrenceDate(), saved.nextOccurrenceDate(), "nextOccurrenceDate should match");
        assertEquals(recurringTransaction.endDate(), saved.endDate(), "endDate should match");
        assertNotNull(saved.createdAt(), "createdAt should not be null");
        assertNotNull(saved.updatedAt(), "updatedAt should not be null");
    }

    // SCENARIO-BASED: assert only what the scenario verifies
    @Test
    void givenNull_shouldThrowInvalidDataAccessApiUsageException() {
        assertThatThrownBy(() -> recurringTransactionRepository.save(null))
                .as("saving null should throw InvalidDataAccessApiUsageException")
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }
}
```

---

## Practical Examples

See [Happy-Path vs Scenario-Based Assertions](#happy-path-vs-scenario-based-assertions) for complete, layer-specific examples (controller, service, repository) that demonstrate the happy-path-every-field rule alongside scenario-based subsequent tests.

The key principle distilled:

```java
@Nested
class Create {

    // FIRST: happy path — assert EVERY field
    @Test
    void givenValidCommand_thenReturnsSummaryWithAllFields() {
        // ... arrange ...

        var result = service.create(command);

        // assert every single field of the result
        assertNotNull(result.id(), "id should be generated");
        assertEquals("expected", result.field(), "field should match");
        // ... all remaining fields ...
    }

    // SUBSEQUENT: scenario-based — assert only what the scenario verifies
    @Test
    void givenInvalidInput_thenThrowsException() {
        // ... arrange ...

        assertThatThrownBy(() -> service.create(invalidCommand))
                .as("should throw when input is invalid")
                .isInstanceOf(ExpectedException.class);
    }
}
```

For void methods, use `ArgumentCaptor` in the happy path:

```java
@Nested
class Delete {

    @Test
    void givenExistingId_thenDelegateToRepository() {
        UUID userId = UUID.randomUUID();
        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);

        userService.delete(userId);

        verify(userRepository).deleteById(idCaptor.capture());
        assertEquals(userId, idCaptor.getValue(), "Repository should receive the correct user id");
    }
}
```

## Checklist

- every method under test has its scenarios grouped under a `@Nested` class
- `@Nested` class name is PascalCase derived from the method under test
- test names follow `given<state>_then<expected_behavior>` (no method name repetition)
- **happy-path test is first and asserts every field of the result (all layers: controller, service, repository)**
- subsequent (non-happy-path) tests assert only what the scenario verifies (scenario-based)
- assertions include concise messages
- JUnit assertions used for simple checks
- AssertJ used for complex checks and collection assertions
- parameterized tests used where applicable
- null/empty input covered with `@NullAndEmptySource` when relevant
- `ArgumentCaptor` used for verifying parameters on void methods
- happy-path test placed first within each `@Nested` class, followed by negative/error scenarios
- `@Nested` classes are ordered by method under test
- test scope matches the layer purpose (unit vs `@WebMvcTest` vs `@SpringBootTest` vs `@DataJpaTest`)
- no comments in test methods
