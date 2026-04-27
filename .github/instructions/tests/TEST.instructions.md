---
applyTo: '**/*Test.java'
description: 'Unit test guidelines for this repository'
---

## Writing Unit Tests Best Practices

### Do

- Use static `mock()` to mock dependencies. e.g., `private final UserRepository userRepository = mock(UserRepository.class);`
- Prefer method names that capture behavior and scenario, e.g., `findById_givenValidId_thenReturnEntity`, `save_givenExistingUser_thenThrowUserAlreadyExistsException`.
- Keep the test body readable with visible given/when/then separation using blank lines.
- Use assertion messages for JUnit assertions when they improve failure diagnostics.
  - When using assertEquals(expected, actual, message), use the field name as the message, for example `assertEquals(expectedBalance, account.getBalance(), "balance")`.
  - When using assertNotNull(object, message), use the field name as the message, for example `assertNotNull(user.id(), "id should not be null")`.
- Use AssertJ for richer object and collection assertions, and JUnit assertions for simple checks.
- Use `@ParameterizedTest` when validating repeated edge-case inputs.
  - Use `NullAndEmptySource` for null and empty string cases.
  - Use `ValueSource` for other edge-case values, for example `@ValueSource(strings = {" ", "\t", "\n"})` for whitespace cases.
  - Use `CsvSource` for multiple related edge-case values, for example `@CsvSource({" ,true", "\t,false", "\n,true"})` for whitespace and expected boolean results.
- Use `@BeforeEach` to set up mock objects and test fixtures to avoid duplication across tests.
- Use `@Nested` test classes to group related tests for the same method or scenario when it improves organization and readability. e.g., a nested class for testing `createUser` method with various edge cases.

### Don't

- Don't use vague or generic test names that hide the scenario or expected outcome.
- Don't compress the whole test into one dense block.
- Don't add comments in test bodies when naming can make the intent clear.
- Don't default to JUnit assertions for object or collection assertions that are clearer in AssertJ.
- Don't duplicate nearly identical tests for the same behavior across multiple edge-case inputs.
