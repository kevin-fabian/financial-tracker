---
name: write-springboot-integration-test
description: Procedure and patterns for writing full @SpringBootTest integration tests for controller layers in this project, including JWT auth setup, required MockitoBean overrides, and test data helpers.
---

# SpringBootTest Controller Integration Test Skill

Primary goal: write full-stack `@SpringBootTest` controller integration tests that exercise the real service and persistence layers behind a `MockMvc` request, with correct JWT-based security context.

Use this when converting a sliced `@WebMvcTest` into a full integration test, or when creating a new end-to-end controller test that runs against the real application context.

## When to Use

- You need to test controller behavior with the real service, repository, and database layers wired together.
- You are converting a `@WebMvcTest` (mocked services) into a `@SpringBootTest` (real services).
- The endpoint is protected by the OAuth2 resource server and requires a JWT with authorities.

## Test Skeleton

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class <ControllerName>SpringBootTest {
    @Autowired
    private MockMvc mockMvc;

    // --- Required: always override these three beans or the context fails to start ---
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    // --- Real services used for setup and (optionally) direct assertions ---
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private TransactionService transactionService;
    // ... other services as needed

    @Autowired
    private JsonMapper jsonMapper;

    // --- Test methods grouped by endpoint under @Nested classes ---
}
```

## Mandatory Rules

### Required MockitoBean overrides

The following three beans **must** be declared as `@MockitoBean` in every SpringBootTest controller test, or the application context will fail to start:

```java
@MockitoBean
private JwtDecoder jwtDecoder;
@MockitoBean
private ClientRegistrationRepository clientRegistrationRepository;
@MockitoBean
private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;
```

Do not remove them even if the test doesn't directly reference them.

### Profile

Always use `@ActiveProfiles("test")` so the test runs against the in-memory H2 database.

### Test data setup via services

Set up domain state by calling the **real services**, not by injecting repositories or persisting entities directly. This keeps tests at the service boundary and avoids coupling to persistence internals. Return values (e.g. IDs) from service calls are captured and used in request URLs and assertions.

Example helpers:

```java
private Category createCategory(UUID userId, String name, TransactionType type, String icon) {
    return categoryService.createCategory(CreateCategoryCommand.builder()
            .name(name)
            .type(type)
            .icon(icon)
            .userId(userId)
            .build());
}

private Account createAccount(UUID userId, String name) {
    return accountService.createAccount(CreateAccountCommand.builder()
            .name(name)
            .currency(Currency.getInstance("USD"))
            .type(AccountType.CASH)
            .userId(userId)
            .build());
}

private void createTransaction(Account account, Category category, double amount) {
    transactionService.addTransaction(AddTransactionCommand.builder()
            .amount(Amount.of(amount, "USD"))
            .transactionDate(LocalDate.now())
            .categoryId(category.id())
            .accountId(account.id())
            .userId(account.userId())
            .build());
}
```

Use services that already exist in the project. Reuse helper methods across `@Nested` classes by placing them at the bottom of the outer test class.

### JWT on requests

Every request to a protected endpoint must carry a JWT. Use this exact post-processor pattern:

```java
.with(jwt()
        .authorities(new SimpleGrantedAuthority("USER"))
        .jwt(jwt -> jwt
                .audience(List.of("zeny-app-password"))
                .claim("sub", userId)
                .claim("scope", List.of())
        ))
```

- `sub` claim sets the authenticated user ID — the controller reads it as the owner.
- `scope` is set to an empty list by default; override when the endpoint requires a specific scope.
- `authorities(new SimpleGrantedAuthority("USER"))` grants the role required by protected endpoints.

Do **not** let clients send ownership identifiers in the payload — the `sub` claim is the only source of user identity.

### Negative auth test cases

For each endpoint group, include both:

1. **No JWT at all** — omit `.with(jwt()...)` entirely, expect `status().isForbidden()`.
2. **JWT with no authorities** — keep the JWT but drop `.authorities(...)`, expect `status().isForbidden()`.

```java
// No JWT
mockMvc.perform(post("/api/budgets")
        .contentType("application/json")
        .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());

// JWT present, but no authorities
mockMvc.perform(post("/api/budgets")
        .with(jwt()
                .jwt(jwt -> jwt
                        .audience(List.of("zeny-app-password"))
                        .claim("sub", userId)
                        .claim("scope", List.of())
                ))
        .contentType("application/json")
        .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
```

### Grouping and naming

- Group tests by endpoint under a `@Nested` class named after the HTTP action in PascalCase (e.g. `GetBudgets`, `CreateBudget`, `PatchBudget`, `DeleteBudget`).
- Test method names follow `given<state>_then<expected_behavior>`.
- Place the happy-path test first, then positive variants, then negative/error scenarios.
- Order `@Nested` classes by the natural lifecycle of the resource (read → create → update → delete), or by method under test.

### Assertions

- For happy-path scenarios, assert all possible fields of the response body.
- Use `jsonPath` to verify individual fields.
- For `201 Created`, assert the `Location` header pattern.
- For `204 No Content`, assert the status only.

### Imports

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; // get, post, patch, delete
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
```

## Workflow

1. Identify the controller and its endpoints to test.
2. Create a `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` test class.
3. Add the three required `@MockitoBean` overrides.
4. `@Autowired` the `MockMvc`, the `JsonMapper`, and any real services needed for test data setup.
5. Write private helper methods that call the real services to create domain state.
6. For each endpoint, create a `@Nested` class with happy-path and negative test methods.
7. Add a JWT post-processor to every request that targets a protected endpoint.
8. Add a "no JWT" and a "JWT with no authorities" negative test for each endpoint group.
9. Run the test class and verify all scenarios pass.

## Practical Example

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BudgetControllerSpringBootTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private JsonMapper jsonMapper;

    @Nested
    class CreateBudget {
        @Test
        void givenValidRequest_thenShouldReturnCreated() throws Exception {
            UUID userId = UUID.randomUUID();
            Category category = createCategory(userId, "GROCERIES", TransactionType.EXPENSE, "local_grocery_store");

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(category.id())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("USER"))
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.period").value("MONTHLY"));
        }

        @Test
        void givenNoJwt_thenShouldReturnForbidden() throws Exception {
            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenJwtWithNoAuthorities_thenShouldReturnForbidden() throws Exception {
            UUID userId = UUID.randomUUID();

            CreateBudgetRequest request = CreateBudgetRequest.builder()
                    .period(BudgetPeriod.MONTHLY)
                    .categoryId(UUID.randomUUID())
                    .allocated(500.0)
                    .build();

            mockMvc.perform(post("/api/budgets")
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .audience(List.of("zeny-app-password"))
                                            .claim("sub", userId)
                                            .claim("scope", List.of())
                                    ))
                            .contentType("application/json")
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    private Category createCategory(UUID userId, String name, TransactionType type, String icon) {
        return categoryService.createCategory(CreateCategoryCommand.builder()
                .name(name).type(type).icon(icon).userId(userId)
                .build());
    }
}
```

## Checklist

- `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` present
- `JwtDecoder`, `ClientRegistrationRepository`, `OAuth2AuthorizedClientRepository` overridden as `@MockitoBean`
- `MockMvc` and `JsonMapper` autowired
- Real services autowired for test data setup (no direct repository/entity persistence)
- Domain state created via service calls, IDs captured from return values
- Every protected request carries a JWT with `USER` authority via the standard post-processor
- Each endpoint group has a "no JWT" negative test and a "JWT with no authorities" negative test
- Test methods grouped under `@Nested` classes named after the action (PascalCase)
- Test names follow `given<state>_then<expected_behavior>`
- Happy-path test placed first within each `@Nested` class
- Happy-path assertions cover all response fields
- No comments in test methods
