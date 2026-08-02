---
name: write-rest-api-controller
description: Workflow and best practices for writing Spring REST APIs with thin controllers, validated web DTOs, service-command/query mapping, focused web-layer tests, and action-oriented endpoints (trailing actions + intent-specific resources).
---

# REST API Best-Practice Skill

Primary goal: write REST endpoints that keep HTTP concerns in controllers, business logic in services, and persistence details behind repository adapters. Includes guidance for action-oriented endpoints (trailing actions and intent-specific resources) with correct HTTP status semantics.

References:
- `.agents/skills/write-domain-services/SKILL.md`
- `.agents/skills/write-jpa-domain-repositories/SKILL.md`
- `.agents/skills/write-jpa-entities/SKILL.md`
- `.agents/skills/write-test-cases/SKILL.md`

## Workflow

1. Scan the existing resource slice in `web`, `services`, and `persistence` before creating any new class.
2. Decide whether the endpoint is **standard CRUD** or an **action** (state change / business event). If an action, choose a pattern (Trailing Action or Intent-Specific Resource — see Action Patterns below).
3. Confirm the HTTP contract, ownership/auth context source, expected status codes, and error behavior.
4. Define or update request/response DTOs in the web layer and add boundary validation with `jakarta.validation` and `@Valid`.
5. Implement or extend the controller to map request DTOs to service commands/queries and map domain results to response DTOs.
6. Keep the controller thin: no business rules, orchestration, or direct repository access.
7. Extend service and persistence layers only when the existing slice cannot satisfy the requested endpoint behavior.
8. Preserve ownership and tenant boundaries by deriving context from trusted server-side authentication, never from client payload.
9. Add focused tests for touched layers, with `@WebMvcTest` for controller-slice behavior.

---

## Mandatory Rules

- Reuse existing `*Controller`, `*Service`, `*Repository`, DTOs, commands, queries, and mappers for the same resource before creating a new slice.
- Controllers must be HTTP adapters only: request mapping, auth/context extraction, DTO mapping, and service invocation.
- Keep request/response contracts at the web boundary as web DTOs; do not expose entities from controller APIs.
- Document public endpoints and DTOs with Swagger v3 annotations (`@Operation`, `@ApiResponse`, `@Schema`) for method, request body, and response.
- For `4xx` and `5xx` `@ApiResponse` entries, document the error body with `@Content(schema = @Schema(implementation = ProblemDetail.class))` so the OpenAPI spec reflects the `ProblemDetail` payload returned by the global exception handler.
- Use explicit mapping: `Request -> Command/Query` and `Domain Model -> Response`.
- Keep business validation and domain rules in services/domain models, not in controllers.
- Never accept ownership identifiers (`userId`, tenant id, organization id) from client request payloads when server context is available.
- Keep persistence mapping and infrastructure concerns in persistence adapters.
- Standard CRUD uses HTTP verbs (GET/PUT/PATCH/DELETE), not trailing verbs — see Action Patterns below.

---

## Action Patterns

Use these patterns when an operation represents a complex state change or business event rather than simple data modification.

### Pattern A: Trailing Actions (Sub-path Verbs)

For simple state transitions, business events, or processes that modify the parent resource directly without needing to track the action itself as a separate entity.

- **HTTP Method:** `POST` (preferred for triggering actions/side-effects).
- **Spring Boot Mapping:** `@PostMapping("/{id}/activate")`.
- **Response:** `200 OK` (returning the updated resource) or `204 No Content` (no body).

> **Example:** `POST /api/accounts/12345/activate`

### Pattern B: Intent-Specific Resources (Lifecycle Entities)

When the action generates side effects that must be audited, requires its own metadata (who approved it, why, when), or kicks off a complex asynchronous workflow, treat the action as a **resource creation**.

- **HTTP Method:** `POST`.
- **Spring Boot Mapping:** `@PostMapping`.
- **Response:** `201 Created` (returns the new entity with its own ID and audit fields).

> **Example:** `POST /api/account-activations`
> **Request Body:** `{"accountId": 12345, "reason": "Completed compliance check"}`

### Decision Matrix

| Use Case | Recommended Pattern | Spring Boot Example |
|---|---|---|
| Simple state toggle (e.g., Active ↔ Suspended) with no extra payload needed. | **Pattern A: Trailing Action** | `POST /api/accounts/{id}/activate` |
| The action requires a strict audit trail or history log in the database. | **Pattern B: Intent Resource** | `POST /api/account-activations` |
| The action triggers a long-running, asynchronous background job. | **Pattern B: Intent Resource** | `POST /api/video-exports` |
| Triggering an event in a state machine (e.g., approving a loan). | **Pattern A: Trailing Action** | `POST /api/loans/{id}/approve` |

### HTTP Status Code Semantics

#### Success (2xx)

| Code | When to Use |
|---|---|
| `200 OK` | Pattern A when returning the newly modified parent resource. |
| `201 Created` | Pattern B when an action results in a new resource record; also standard CRUD create. |
| `202 Accepted` | Long-running asynchronous background task. |
| `204 No Content` | Pattern A when the action completes successfully with no response body. |

#### Client Errors (4xx)

| Code | When to Use |
|---|---|
| `400 Bad Request` | Request payload failed validation. |
| `401 Unauthorized` | Client is not authenticated. |
| `403 Forbidden` | Client is authenticated but lacks permission for this action. |
| `404 Not Found` | Resource specified by ID does not exist. |
| `409 Conflict` | Action conflicts with current resource state (e.g., activating an already-active account). |

#### Server Errors (5xx)

| Code | When to Use |
|---|---|
| `500 Internal Server Error` | Generic catch-all for unhandled exceptions. |

### Anti-Patterns to Avoid

- ❌ **Don't use URL-based API versioning (`/v1/`):** Use header-based versioning (`X-API-VERSION`) or media type negotiation.
- ❌ **Don't mix verbs into standard CRUD paths:** Avoid `POST /api/accounts/create` or `POST /api/accounts/{id}/delete`. Use standard HTTP methods.
- ❌ **Don't use GET for actions:** `GET` must remain safe, side-effect-free, and cacheable.

---

## Practical: Step 1 - Define HTTP contracts and validation

### Standard CRUD request DTO

```java
@Builder(toBuilder = true)
@Schema(description = "Request to create an item")
public record CreateItemRequest(
    @Schema(description = "Item name", example = "Premium Widget")
    @NotBlank(message = "name is required")
    @Size(max = 128, message = "name must not exceed 128 characters")
    String name,
    @Schema(description = "Item price", example = "49.99")
    @NotNull(message = "price is required")
    BigDecimal price
) {
    public CreateItemCommand toCommand(UUID userId) {
        return CreateItemCommand.builder()
            .name(name)
            .price(price)
            .userId(userId)
            .build();
    }
}
```

### Response DTO

```java
@Builder(toBuilder = true)
@Schema(description = "Response for a created item")
public record ItemResponse(
    @Schema(description = "Item identifier", example = "2e7af8d1-5c2e-4d53-b1ca-4f6fcae334b5")
    UUID id,
    @Schema(description = "Item name", example = "Premium Widget")
    String name,
    @Schema(description = "Item price", example = "49.99")
    BigDecimal price
) {
    public static ItemResponse from(Item item) {
        return ItemResponse.builder()
            .id(item.getId())
            .name(item.getName())
            .price(item.getPrice())
            .build();
    }
}
```

### Action-specific request/command DTO (Pattern B)

```java
@Builder(toBuilder = true)
@Schema(description = "Request to activate an account")
public record CreateAccountActivationRequest(
    @Schema(description = "Target account ID")
    @NotNull(message = "accountId is required")
    UUID accountId,
    @Schema(description = "Reason for activation")
    String reason
) {
    public AccountActivationCommand toCommand(UUID userId) {
        return AccountActivationCommand.builder()
            .accountId(accountId)
            .reason(reason)
            .requestedBy(userId)
            .build();
    }
}
```

Guidance:
- Keep DTOs close to controllers.
- Put validation annotations on request DTOs at the boundary.
- Add a `@Size(max = ...)` to **every** `String` field (use a sensible limit per field, e.g. 36 for a unit, 128 for names/categories, 255 for descriptions) so oversized payloads are rejected with `400` at the boundary rather than reaching the persistence layer.
- Add field-level `@Schema` examples/descriptions so request payloads are self-documented in OpenAPI.
- Action Pattern B endpoints look like standard CRUD `POST` endpoints — they create a new resource.

---

## Practical: Step 2 - Keep controller logic thin

### Standard CRUD controller

```java
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new item",
        description = "Creates a new item and returns the created entity.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created - Item created.",
                content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        }
    )
    public ItemResponse create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateItemRequest request
    ) {
        Item item = itemService.create(request.toCommand(principal.userId()));
        return ItemResponse.from(item);
    }
}
```

### Trailing action controller (Pattern A)

```java
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/{id}/activate")
    @Operation(
        summary = "Activate an account",
        description = "Activates the specified account. Returns 204 on success.",
        responses = {
            @ApiResponse(responseCode = "204", description = "No Content - Account activated."),
            @ApiResponse(responseCode = "404", description = "Not Found - Account not found.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Account is already active.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        }
    )
    public ResponseEntity<Void> activateAccount(
        @PathVariable Long id,
        JwtAuthenticationToken jwtAuthenticationToken) {

        UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
        accountService.activate(id, userId);

        return ResponseEntity.noContent().build();
    }
}
```

### Intent-specific resource controller (Pattern B)

```java
@RestController
@RequestMapping("/api/account-activations")
@RequiredArgsConstructor
public class AccountActivationController {

    private final AccountActivationService accountActivationService;

    @PostMapping
    @Operation(
        summary = "Request account activation",
        description = "Creates a new account activation request for audit tracking.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Created - Activation request created.",
                content = @Content(schema = @Schema(implementation = AccountActivationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure.",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
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
- Controllers translate HTTP input/output and delegate business behavior to services.
- Ownership context comes from authenticated server-side context (`JwtAuthenticationToken`).
- Keep `@Operation` + `@ApiResponse` aligned with actual status and error behavior.

---

## Practical: Step 3 - Service layer

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

## Practical: Step 4 - Focused tests

### Controller test (`@WebMvcTest`)

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

### Parameterized field-validation tests

When several request fields share the same validation rule (e.g. `@NotBlank`, `@Size(max = ...)`), cover the failing cases with a single `@ParameterizedTest` driven by a `@MethodSource` that supplies one invalid request per field. Keep the happy-path and not-found tests as separate `@Test` methods.

```java
@ParameterizedTest
@NullAndEmptySource
void givenBlankName_thenReturnsBadRequest(String name) throws Exception {
    CreateItemRequest request = CreateItemRequest.builder()
            .name(name)
            .category("Dairy")
            .quantity(2.0)
            .unit("liters")
            .price(3.5)
            .priority(ItemPriority.HIGH)
            .build();

    mockMvc.perform(post("/api/items/{id}/items", UUID.randomUUID())
                    .with(jwt()
                            .authorities(new SimpleGrantedAuthority("USER"))
                            .jwt(jwt -> jwt
                                    .audience(List.of("financial-tracker-test"))
                                    .claim("sub", UUID.randomUUID())
                                    .claim("scope", List.of())
                            ))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@ParameterizedTest
@MethodSource("oversizedFieldRequests")
void givenFieldExceedsMaxLength_thenReturnsBadRequest(CreateItemRequest request) throws Exception {
    mockMvc.perform(post("/api/items/{id}/items", UUID.randomUUID())
                    .with(jwt()
                            .authorities(new SimpleGrantedAuthority("USER"))
                            .jwt(jwt -> jwt
                                    .audience(List.of("financial-tracker-test"))
                                    .claim("sub", UUID.randomUUID())
                                    .claim("scope", List.of())
                            ))
                    .contentType("application/json")
                    .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

static Stream<Arguments> oversizedFieldRequests() {
    return Stream.of(
            Arguments.of(CreateItemRequest.builder()
                    .name("a".repeat(129))
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .build()),
            Arguments.of(CreateItemRequest.builder()
                    .name("Milk")
                    .category("a".repeat(129))
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .priority(ItemPriority.HIGH)
                    .build()),
            Arguments.of(CreateItemRequest.builder()
                    .name("Milk")
                    .category("Dairy")
                    .quantity(2.0)
                    .unit("liters")
                    .price(3.5)
                    .notes("a".repeat(129))
                    .priority(ItemPriority.HIGH)
                    .build())
    );
}
```

Guidance:
- Use `@NullAndEmptySource` to cover both `null` and `""` (and `@NotBlank` also rejects whitespace-only) in one method.
- Use `@MethodSource` with a `static Stream<Arguments>` when each failing case needs a different field populated differently (e.g. exceeding a per-field `@Size` max).
- Build every supplied request so that **only one** field violates at a time — keep the rest valid — to pinpoint which annotation rejects.
- Name the method `given<Field>ExceedsMaxLength_thenReturnsBadRequest` (or `givenBlank<Field>_thenReturnsBadRequest`) so the failure message identifies the rule.

### Service test (unit)

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
- Controller tests use `@WebMvcTest(ControllerClass.class)` with `@MockitoBean` for the service and `MockMvc` for HTTP assertions.
- Service tests instantiate the concrete service with mocked collaborators.
- Verify both success and error paths (not found, conflict, unauthorized).

---

## Checklist

- existing resource slice is reused where possible before creating new classes
- controller is thin and contains no business logic or repository access
- request/response models at HTTP boundary are web DTOs
- request validation is applied at DTO/controller boundary
- swagger v3 annotations cover method, request body DTO, and response DTO
- `4xx` and `5xx` `@ApiResponse` entries document the error body with `@Content(schema = @Schema(implementation = ProblemDetail.class))`
- ownership context is derived from trusted server-side authentication context
- mapping is explicit (`Request -> Command/Query`, `Domain -> Response`)
- services and persistence layers are extended only when needed
- touched layers have focused tests, including `@WebMvcTest` for controllers
- standard CRUD uses HTTP verbs (GET/PUT/PATCH/DELETE), not trailing verbs
- complex state changes or business events use trailing actions (`POST /{id}/activate`) or intent resources (`POST /api/activations`)
- trailing actions use `POST` with `200 OK` (returning resource) or `204 No Content` (no body)
- intent resources use `POST` with `201 Created` and return the new entity
- long-running actions return `202 Accepted`
- `409 Conflict` for state conflicts (e.g., activating an already-active account)
- `404 Not Found` for missing resources
- service owns business rules, cross-user isolation, and domain exceptions
- controller tests use `@WebMvcTest` with `MockMvc` and `@MockitoBean`
- request field-validation failures are covered by `@ParameterizedTest` (`@NullAndEmptySource` or `@MethodSource`), one invalid field per supplied request
- every `String` request field carries a `@Size(max = ...)` boundary annotation
- service tests use mocked collaborators with focused assertions
