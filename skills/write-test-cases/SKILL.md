---
name: write-test-cases
description: Workflow and best practices for writing clear, consistent, and maintainable Java test cases across all test layers. The happy-path test MUST assert every field of the result — this is non-negotiable for controller, service, and repository layers.
---

# Test Case Best-Practice Skill

Primary goal: write test cases that are readable, behavior-focused, and consistent across the codebase for all test
layers (`@WebMvcTest`, `@SpringBootTest`, unit tests, `@DataJpaTest`, and similar).

## Rules

### Grouping and Naming

- Group all test scenarios for a method under a `@Nested` class named after the method in PascalCase (e.g.
  `createUser` → `@Nested class CreateUser`, `organize` → `OrganizeParty`).
- Name each test method using `given<state>_shouldReturn<expected_behavior>` — omit the method name, since the `@Nested`
  class already provides that context.
- Order `@Nested` classes by method under test.
- Within each `@Nested` class, place the happy-path test first, followed by positive and negative/error scenarios.

### Happy-Path Field Coverage (all layers)

The first test case for every method MUST be the happy path and MUST assert **every field** of the returned result — no
exceptions. This applies to controller (`@WebMvcTest`, `@SpringBootTest`), service (unit), and repository
(`@DataJpaTest`) layers. Use `jsonPath` for controller tests, direct field access for service tests, and entity field
assertions for repository tests. Subsequent (non-happy-path) scenarios assert only what the scenario verifies —
scenario-based assertions, not full-field enumeration.

### Assertions

- Use JUnit 5 assertions for simple checks (`assertEquals`, `assertNotNull`, `assertTrue`, etc.).
- Use AssertJ for complex assertions or when asserting collections (size/content/order, recursive comparison, chained
  extraction).
- Include concise assertion messages for intent clarity.
- Use `ArgumentCaptor` to verify parameters passed to collaborators for methods with no return value.
- Use `verify(collaborator).method(args)` to confirm a collaborator was called.
- Use `verify(collaborator, never()).method(args)` in negative tests to confirm no side effects occurred.

### Mocking and Setup

- Unit tests: use `mock()` for collaborator setup. Declare the service under test as a `final` field and instantiate it
  manually with mocked collaborators — do not use `@ExtendWith(MockitoExtension.class)` or `@Mock`.
- Spring slice tests (`@WebMvcTest`, `@SpringBootTest`): use `@MockitoBean` (Spring Boot 3.4+) to override beans. Do not
  use the deprecated `@MockBean`.
- `@DataJpaTest`: provide adapter beans in a nested `@TestConfiguration` class.

### Spring Test Slice Requirements

- Any Spring test slice MUST import `@Import(CacheTestConfig.class)` to enable caching in tests; otherwise test cases
  will fail. See `src/test/java/com/fabiankevin/app/config/CacheTestConfig.java`.
- `@SpringBootTest` integration tests MUST use `@ActiveProfiles("test")` so the test runs against the in-memory H2
  database.
- `@SpringBootTest` controller tests MUST override `JwtDecoder`, `ClientRegistrationRepository`, and
  `OAuth2AuthorizedClientRepository` as `@MockitoBean`, or the application context will fail to start.

### Parameterization

- Use `@ParameterizedTest` with `@ValueSource`, `@CsvSource`, or `@EnumSource` when inputs vary systematically.
- Use `@NullAndEmptySource` when validating null/empty string behavior.
- Convert repetitive scenario matrices into parameterized tests.

### Style

- Execute one clear behavior per test.
- Keep clear blank-line separation between setup, execution, and assertion blocks.
- Strictly no comments inside test cases.

### Test Count per Method Type

| Method type                                                 | Minimum tests | Guidance                                                                                 |
|-------------------------------------------------------------|---------------|------------------------------------------------------------------------------------------|
| Pure delegation (no logic, returns repository result as-is) | 2             | One happy path (non-empty result) + one empty/default result.                            |
| Common repository methods (`save`, `retrieve*`, `delete*`)  | 2–3           | Happy path + not-found case + edge case (null input, empty collection, etc.).            |
| Methods with domain logic                                   | 5+            | One happy path + all exception branches + edge cases. Cover every `throw` in the method. |

---

## Layer Applicability

- **Unit tests**: isolate business logic and mock external collaborators.
- **`@WebMvcTest`**: focus on controller request/response behavior, validation, status codes, and JSON contracts.
- **`@SpringBootTest`**: full application context with real database (e.g. H2) for integration tests that verify the
  complete stack — controllers, services, repositories, and data persistence.
- **`@DataJpaTest`**: focus on repository persistence behavior, query correctness, and entity mapping.
- For any layer, keep each test focused on one behavior and assert both success and failure scenarios where relevant.

---

## Happy-Path vs Scenario-Based Assertions

The first test case for every method MUST be the happy path and MUST assert **every field** of the returned result.
Subsequent scenarios assert only what the scenario verifies. This applies to all layers.

### Controller Layer (`@WebMvcTest`)

Use `jsonPath` to assert every field of the response DTO in the happy path:

```java

@Nested
class GetRecurringTransactions {

    @Test
    void givenRecurringTransactionsExist_shouldReturnListOfSummaries() throws Exception {
        mockMvc.perform(get("/api/recurring-transactions")
                .with(jwt()...))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].description").value("Monthly subscription"))
                .andExpect(jsonPath("$[0].amount").value(15.99))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists());
    }

    @Test
    void givenNoRecurringTransactions_shouldReturnEmptyList() throws Exception {
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

    @Test
    void givenValidRequest_shouldReturnCreatedWithSummary() throws Exception {
        mockMvc.perform(post("/api/recurring-transactions")...)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value("Monthly subscription"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void givenNoJwt_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/recurring-transactions")...)
                .andExpect(status().isForbidden());
    }

    @Test
    void givenNonExistentAccount_shouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/recurring-transactions")...)
                .andExpect(status().isNotFound());
    }
}
```

### Integration Layer (`@SpringBootTest`)

Full application context with a real database (e.g. H2) for integration tests that verify the complete stack —
controllers, services, repositories, and data persistence. Use `@SpringBootTest` +
`@AutoConfigureTestDatabase(replace = Replace.NONE)` or an in-memory database for tests that need real persistence.

```java

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CacheTestConfig.class)
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class RegisterUser {

        @Test
        void givenValidRegistrationData_shouldReturnCreatedUser() throws Exception {
            var request = new CreateUserRequest("john@example.com", "John", "Doe", "Password123!");

            mockMvc.perform(post("/api/users")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        void givenDuplicateEmail_shouldReturnConflict() throws Exception {
            var request = new CreateUserRequest("john@example.com", "Jane", "Doe", "Password123!");

            mockMvc.perform(post("/api/users")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void givenWeakPassword_shouldReturnBadRequest() throws Exception {
            var request = new CreateUserRequest("jane@example.com", "Jane", "Doe", "weak");

            mockMvc.perform(post("/api/users")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
```

### Service Layer (Unit Test)

Declare the service under test as a `final` field and use manual `mock()` for collaborators — do not use
`@ExtendWith(MockitoExtension.class)` or `@Mock`.

```java
class DefaultPurchaseVerificationServiceTest {

    private final PurchaseVerificationService service = new DefaultPurchaseVerificationService(
            accountRepository,
            purchaseRepository,
            fraudDetectionClient
    );

    private final Account account = AccountTestHelpers.createActiveAccount();
    private final Purchase purchase = PurchaseTestHelpers.createPurchase(account);

    @Nested
    class VerifyPurchase {

        @Test
        void givenValidPurchase_shouldReturnVerifiedResult() {
            when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));
            when(purchaseRepository.findById(purchase.id())).thenReturn(Optional.of(purchase));
            when(fraudDetectionClient.check(any())).thenReturn(FraudCheckResult.CLEAR);

            PurchaseVerificationResult result = service.verifyPurchase(purchase.id());

            assertNotNull(result, "result should not be null");
            assertTrue(result.isVerified(), "result should be verified");
            assertEquals(purchase.id(), result.purchaseId(), "purchaseId should match");
            assertNotNull(result.verifiedAt(), "verifiedAt should not be null");

            verify(accountRepository).findById(account.id());
            verify(purchaseRepository).findById(purchase.id());
            verify(fraudDetectionClient).check(any());
        }

        @Test
        void givenNonExistentAccount_shouldThrowAccountNotFoundException() {
            when(accountRepository.findById(account.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyPurchase(purchase.id()))
                    .as("should throw AccountNotFoundException when account does not exist")
                    .isInstanceOf(AccountNotFoundException.class);

            verify(purchaseRepository, never()).findById(any());
            verify(fraudDetectionClient, never()).check(any());
        }

        @Test
        void givenFraudFlagged_shouldThrowFraudDetectedException() {
            when(accountRepository.findById(account.id())).thenReturn(Optional.of(account));
            when(purchaseRepository.findById(purchase.id())).thenReturn(Optional.of(purchase));
            when(fraudDetectionClient.check(any())).thenReturn(FraudCheckResult.FRAUD);

            assertThatThrownBy(() -> service.verifyPurchase(purchase.id()))
                    .as("should throw FraudDetectedException when fraud is flagged")
                    .isInstanceOf(FraudDetectedException.class);
        }
    }
}
```

### Repository Layer (`@DataJpaTest`)

Assert every persisted field in the happy path. Provide adapter beans in a nested `@TestConfiguration` class.

```java

@DataJpaTest
@Import(CacheTestConfig.class)
class RecurringTransactionRepositoryTest {

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @TestConfiguration
    static class AdapterConfig {
        @Bean
        RecurringTransactionRepository recurringTransactionRepository(JpaRecurringTransactionRepository jpaRepo) {
            return new DefaultRecurringTransactionRepository(jpaRepo);
        }
    }

    @Nested
    class Save {

        @Test
        void givenValidRecurringTransaction_persistsAndRetrievesAllFields() {
            RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);

            assertNotNull(saved.id(), "recurring transaction id should have been generated");
            assertEquals(recurringTransaction.description(), saved.description(), "description should match");
            assertEquals(recurringTransaction.amount(), saved.amount(), "amount should match");
            assertNotNull(saved.createdAt(), "createdAt should not be null");
            assertNotNull(saved.updatedAt(), "updatedAt should not be null");
        }

        @Test
        void givenNull_shouldThrowInvalidDataAccessApiUsageException() {
            assertThatThrownBy(() -> recurringTransactionRepository.save(null))
                    .as("saving null should throw InvalidDataAccessApiUsageException")
                    .isInstanceOf(InvalidDataAccessApiUsageException.class);
        }
    }
}
```

---

## Checklist

- Every method under test has its scenarios grouped under a `@Nested` class
- `@Nested` class name is PascalCase derived from the method under test
- Test names follow `given<state>_shouldReturn<expected_behavior>` (no method name repetition)
- Happy-path test is first and asserts every field of the result (all layers: controller, service, repository)
- Subsequent (non-happy-path) tests assert only what the scenario verifies (scenario-based)
- Assertions include concise messages
- JUnit assertions used for simple checks; AssertJ used for complex checks and collection assertions
- `verify(...)` used to confirm collaborator calls; `verify(never())` used in negative tests
- `ArgumentCaptor` used for verifying parameters on void methods
- Parameterized tests used where applicable; `@NullAndEmptySource` for null/empty input
- `@Nested` classes are ordered by method under test
- Test scope matches the layer purpose (unit vs `@WebMvcTest` vs `@SpringBootTest` vs `@DataJpaTest`)
- Spring test slices import `@Import(CacheTestConfig.class)`
- `@SpringBootTest` uses `@ActiveProfiles("test")` and overrides `JwtDecoder`, `ClientRegistrationRepository`,
  `OAuth2AuthorizedClientRepository` as `@MockitoBean`
- `@DataJpaTest` provides adapter beans in a nested `@TestConfiguration`
- No comments in test methods
