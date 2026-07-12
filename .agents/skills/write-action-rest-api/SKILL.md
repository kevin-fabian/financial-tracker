---
name: write-action-rest-api
description: Workflow and best practices for designing action-oriented REST endpoints using trailing actions and intent-specific resources with correct HTTP status semantics.
---

# Action-Oriented REST API Best-Practice Skill

Primary goal: design REST endpoints that follow resource-oriented principles for CRUD, while cleanly handling business-logic actions using **Trailing Actions** or **Intent-Specific Resources**, backed by correct HTTP status code semantics.

References:
- `.github/skills/write-rest-api/SKILL.md`
- `.github/skills/write-domain-services/SKILL.md`
- `.github/skills/write-test-cases/SKILL.md`

---

## The Core Philosophy

1. **Resources first:** Everything is a noun by default.
2. **Standard CRUD uses HTTP verbs:** `GET` (read), `POST` (create), `PUT` (replace), `PATCH` (partial update), `DELETE` (remove).
3. **Actions use verbs at the end:** When an operation represents a complex state change or business event rather than a simple data modification, append the verb to the resource path.

---

## Two Approved Action Patterns

### Pattern A: Trailing Actions (Sub-path Verbs)

For simple state transitions, business events, or processes that modify the parent resource directly without needing to track the action itself as a separate entity.

- **HTTP Method:** `POST` (preferred for triggering actions/side-effects).
- **Spring Boot Mapping:** `@PostMapping("/{id}/activate")`.

> **Example:** `POST /api/accounts/12345/activate`
> **Response:** `200 OK` (returning the updated account) or `204 No Content` (returning nothing).

### Pattern B: Intent-Specific Resources (Lifecycle Entities)

When the action generates side effects that must be audited, requires its own metadata (who approved it, why, when), or kicks off a complex asynchronous workflow, treat the action as a **resource creation**.

- **HTTP Method:** `POST`.
- **Spring Boot Mapping:** `@PostMapping`.

> **Example:** `POST /api/account-activations`
> **Request Body:** `{"accountId": 12345, "reason": "Completed compliance check"}`
> **Response:** `201 Created` (returns the new `AccountActivation` entity with its own ID and audit fields).

---

## Decision Matrix

| Use Case | Recommended Pattern | Spring Boot Example |
|---|---|---|
| Simple state toggle (e.g., Active ↔ Suspended) with no extra payload needed. | **Pattern A: Trailing Action** | `POST /api/accounts/{id}/activate` |
| The action requires a strict audit trail or history log in the database. | **Pattern B: Intent Resource** | `POST /api/account-activations` |
| The action triggers a long-running, asynchronous background job. | **Pattern B: Intent Resource** | `POST /api/video-exports` |
| Triggering an event in a state machine (e.g., approving a loan). | **Pattern A: Trailing Action** | `POST /api/loans/{id}/approve` |

---

## HTTP Status Code Semantics

### Success (2xx)

| Code | When to Use |
|---|---|
| `200 OK` | Pattern A when returning the newly modified parent resource. |
| `201 Created` | Pattern B when an action results in a new resource record. |
| `202 Accepted` | Long-running asynchronous background task. |
| `204 No Content` | Pattern A when the action completes successfully with no response body. |

### Client Errors (4xx)

| Code | When to Use |
|---|---|
| `400 Bad Request` | Request payload failed validation. |
| `401 Unauthorized` | Client is not authenticated. |
| `403 Forbidden` | Client is authenticated but lacks permission for this action. |
| `404 Not Found` | Resource specified by ID does not exist. |
| `409 Conflict` | Action conflicts with current resource state (e.g., activating an already-active account). |

### Server Errors (5xx)

| Code | When to Use |
|---|---|
| `500 Internal Server Error` | Generic catch-all for unhandled exceptions. |

---

## Anti-Patterns to Avoid

- ❌ **Don't use URL-based API versioning (`/v1/`):** Use header-based versioning (`X-API-VERSION`) or media type negotiation.
- ❌ **Don't mix verbs into standard CRUD paths:** Avoid `POST /api/accounts/create` or `POST /api/accounts/{id}/delete`. Use standard HTTP methods.
- ❌ **Don't use GET for actions:** `GET` must remain safe, side-effect-free, and cacheable.

---

## Practical: Step 1 - Trailing Action (Pattern A)

```java
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // Pattern A: Trailing action — returns no content
    @PostMapping("/{id}/activate")
    @Operation(
        summary = "Activate an account",
        description = "Activates the specified account. Returns 204 on success.",
        responses = {
            @ApiResponse(responseCode = "204", description = "No Content - Account activated."),
            @ApiResponse(responseCode = "404", description = "Not Found - Account not found."),
            @ApiResponse(responseCode = "409", description = "Conflict - Account is already active."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure.")
        }
    )
    public ResponseEntity<Void> activateAccount(
        @PathVariable Long id,
        JwtAuthenticationToken jwtAuthenticationToken) {

        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        accountService.activate(id, userId);
        // Throws AccountConflictException (mapped to 409) if already active
        // Throws ResourceNotFoundException (mapped to 404) if ID doesn't exist

        return ResponseEntity.noContent().build();
    }
}
```

Guidance:
- Controllers translate HTTP input/output and delegate business behavior to services.
- Ownership context comes from authenticated server-side context (`JwtAuthenticationToken`).
- Keep `@Operation` + `@ApiResponse` aligned with actual status and error behavior.

---

## Practical: Step 2 - Intent-Specific Resource (Pattern B)

```java
@RestController
@RequestMapping("/api/account-activations")
@RequiredArgsConstructor
public class AccountActivationController {

    private final AccountActivationService accountActivationService;

    // Pattern B: Intent resource — creates a new audit record
    @PostMapping
    @Operation(
        summary = "Request account activation",
        description = "Creates a new account activation request for audit tracking.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created - Activation request created.",
                content = @Content(schema = @Schema(implementation = AccountActivationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure.")
        }
    )
    public AccountActivationResponse createActivation(
        @Valid @RequestBody CreateAccountActivationRequest request,
        JwtAuthenticationToken jwtAuthenticationToken) {

        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        AccountActivation activation = accountActivationService.createActivation(
            request.toCommand(userId));

        return AccountActivationResponse.from(activation);
    }
}
```

Guidance:
- Pattern B endpoints look like standard CRUD `POST` endpoints — they create a new resource.
- The request DTO converts itself to a service command (`CreateAccountActivationRequest#toCommand`).
- Response DTO maps from the domain model (`AccountActivationResponse#from`).

---

## Practical: Step 3 - Service layer with action logic

```java
public interface AccountService {
    void activate(Long accountId, UUID userId);
}

@Service
@RequiredArgsConstructor
public class DefaultAccountService implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void activate(Long accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
            .filter(a -> a.userId().equals(userId))
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (account.isActive()) {
            throw new AccountConflictException("Account is already active");
        }

        accountRepository.save(account.activate());
    }
}
```

Guidance:
- Service methods own business rules and orchestration.
- Cross-user isolation enforced via `.filter(a -> a.userId().equals(userId))`.
- Domain model methods (e.g., `account.activate()`) encapsulate state transitions.
- Throw domain exceptions; let `@ControllerAdvice` handle HTTP mapping.

---

## Practical: Step 4 - Tests

**Controller test (`@WebMvcTest`):**

```java
@WebMvcTest(AccountController.class)
class AccountControllerActivateTest {

    @MockitoBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void activateAccount_withValidId_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/accounts/1/activate")
                .with(jwt()))
            .andExpect(status().isNoContent());

        verify(accountService, times(1)).activate(eq(1L), any(UUID.class));
    }

    @Test
    void activateAccount_withNotFoundAccount_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Account not found: 999"))
            .when(accountService).activate(eq(999L), any(UUID.class));

        mockMvc.perform(post("/api/accounts/999/activate")
                .with(jwt()))
            .andExpect(status().isNotFound());
    }
}
```

**Service test (unit):**

```java
class DefaultAccountServiceActivateTest {

    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountService = new DefaultAccountService(accountRepository);
    }

    @Test
    void activate_withValidAccount_setsActiveFlag() {
        Account account = Account.builder()
            .id(1L)
            .userId(UUID.randomUUID())
            .active(false)
            .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        accountService.activate(1L, account.userId());

        assertTrue(account.isActive());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void activate_withAlreadyActiveAccount_throwsConflict() {
        Account account = Account.builder()
            .id(1L)
            .userId(UUID.randomUUID())
            .active(true)
            .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(AccountConflictException.class,
            () -> accountService.activate(1L, account.userId()));
    }
}
```

Guidance:
- Controller tests use `@WebMvcTest` with `@MockitoBean` for the service and `MockMvc` for HTTP assertions.
- Service tests instantiate the concrete service with mocked collaborators.
- Verify both success and error paths (not found, conflict, unauthorized).

---

## Checklist

- standard CRUD uses HTTP verbs (GET/PUT/PATCH/DELETE), not trailing verbs
- complex state changes or business events use trailing actions (`POST /{id}/activate`) or intent resources (`POST /api/activations`)
- trailing actions use `POST` with `200 OK` (returning resource) or `204 No Content` (no body)
- intent resources use `POST` with `201 Created` and return the new entity
- long-running actions return `202 Accepted`
- `409 Conflict` for state conflicts (e.g., activating an already-active account)
- `404 Not Found` for missing resources
- ownership context derived from `JwtAuthenticationToken`, never from request payloads
- `@Operation` + `@ApiResponse` aligned with actual status codes
- controller is thin: no business rules, no repository access
- service owns business rules, cross-user isolation, and domain exceptions
- controller tests use `@WebMvcTest` with `MockMvc` and `@MockitoBean`
- service tests use mocked collaborators with focused assertions
