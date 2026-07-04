---
name: write-rest-api
description: Workflow and best practices for writing Spring REST APIs with thin controllers, validated web DTOs, service-command/query mapping, and focused web-layer tests.
---

# REST API Best-Practice Skill

Primary goal: write REST endpoints that keep HTTP concerns in controllers, business logic in services, and persistence details behind repository adapters.

References:
- `.github/copilot-instructions.md`
- `.github/skills/write-domain-services/SKILL.md`
- `.github/skills/write-jpa-domain-repositories/SKILL.md`
- `.github/skills/write-jpa-entities/SKILL.md`
- `.github/skills/write-test-cases/SKILL.md`

## Workflow

1. Scan the existing resource slice in `web`, `services`, and `persistence` before creating any new class.
2. Confirm the HTTP contract, ownership/auth context source, expected status codes, and error behavior.
3. Define or update request/response DTOs in the web layer and add boundary validation with `jakarta.validation` and `@Valid`.
4. Implement or extend the controller to map request DTOs to service commands/queries and map domain results to response DTOs.
5. Keep the controller thin: no business rules, orchestration, or direct repository access.
6. Extend service and persistence layers only when the existing slice cannot satisfy the requested endpoint behavior.
7. Preserve ownership and tenant boundaries by deriving context from trusted server-side authentication, never from client payload.
8. Add focused tests for touched layers, with `@WebMvcTest` for controller-slice behavior.

---

## Mandatory Rules

- Reuse existing `*Controller`, `*Service`, `*Repository`, DTOs, commands, queries, and mappers for the same resource before creating a new slice.
- Controllers must be HTTP adapters only: request mapping, auth/context extraction, DTO mapping, and service invocation.
- Keep request/response contracts at the web boundary as web DTOs; do not expose entities from controller APIs.
- Document public endpoints and DTOs with Swagger v3 annotations (`@Operation`, `@ApiResponse`, `@Schema`) for method, request body, and response.
- Use explicit mapping: `Request -> Command/Query` and `Domain Model -> Response`.
- Keep business validation and domain rules in services/domain models, not in controllers.
- Never accept ownership identifiers (`userId`, tenant id, organization id) from client request payloads when server context is available.
- Keep persistence mapping and infrastructure concerns in persistence adapters.

---

## Web DTO Naming Convention

When designing web-layer DTOs for REST endpoints, use a naming convention that signals the DTO's purpose at a glance.

| Endpoint purpose | DTO name suffix | When to use |
|---|---|---|
| **Retrieve / filter** (GET) | `Query` | Any retrieval endpoint that accepts filter parameters (dates, IDs, pagination, etc.) |
| **Create / mutate** (POST / PUT / PATCH / DELETE) | `Command` | When the payload has **2-3 or more fields**, or is expected to grow over time |
| **Create / mutate** (POST / PUT / PATCH / DELETE) | `Request` | When the payload is trivially small (1-2 fields) and unlikely to grow |

- `Query` signals a **read-only filter** — no side effects, just selection criteria.
- `Command` signals an **intent to change state** — it carries enough fields to be a domain command.
- `Request` is a fallback for trivial payloads where the extra naming overhead isn't worth it.

**How to apply:**

1. **GET endpoints** — always name the DTO `*Query`. Example: `StatsQuery`, `FindTransactionsQuery`.
2. **POST/PUT endpoints** — if the payload has 2-3+ fields or is expected to grow, name it `*Command`. Example: `AddTransactionCommand`, `CreateAccountCommand`.
3. **Trivial POST/PUT** (1-2 fields) — `*Request` is acceptable. Example: `RenameCategoryRequest`.

**File naming:** The DTO file name matches the class name (`StatsQuery.java`, `AddTransactionCommand.java`). Keep DTOs in `web/controllers/dtos/`.

---

## Practical: Step 1 - Define HTTP contracts and validation

### Query DTO (GET)

```java
@Builder(toBuilder = true)
@Schema(description = "Query DTO for filtering stats queries")
public record StatsQuery(
        @Schema(description = "Start date for filtering transactions (inclusive)", example = "2025-01-01")
        LocalDate fromDate,

        @Schema(description = "End date for filtering transactions (inclusive)", example = "2025-01-31")
        LocalDate toDate,

        @Schema(description = "Account id to filter by", example = "d290f1ee-6c54-4b01-90e6-d701748f0852")
        UUID accountId,

        @Schema(description = "Category id to filter by", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId
) {
}
```

### Command DTO (POST)

```java
@Builder(toBuilder = true)
@Schema(description = "Command to add a transaction")
public record AddTransactionCommand(
        @Schema(description = "Account id", example = "d290f1ee-6c54-4b01-90e6-d701748f0852")
        UUID accountId,

        @Schema(description = "Category id", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID categoryId,

        @Schema(description = "Transaction amount", example = "50.00")
        @NotNull(message = "amount is required")
        BigDecimal amount,

        @Schema(description = "Transaction date", example = "2025-06-15")
        @NotNull(message = "transactionDate is required")
        LocalDate transactionDate,

        @Schema(description = "User id (from auth context)", hidden = true)
        UUID userId
) {
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

Guidance:
- Keep DTOs close to controllers in `web/controllers/dtos/`.
- Put validation annotations on request DTOs at the boundary.
- Add field-level `@Schema` examples/descriptions so request payloads are self-documented in OpenAPI.
- Request DTOs convert themselves to service commands (e.g., `Request#toCommand` → `Command`).

---

## Practical: Step 2 - Keep controller logic thin

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
            @ApiResponse(responseCode = "201", description = "Created - Item created."),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure.")
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

Guidance:
- Controllers translate HTTP input/output and delegate business behavior to services.
- Ownership context comes from authenticated server-side context.
- Keep `@Operation` + `@ApiResponse` aligned with actual status and error behavior.

---

## Practical: Step 3 - Use `@WebMvcTest` for controller-slice tests

- Use `@WebMvcTest(ControllerClass.class)` for HTTP contract tests.
- Mock controller collaborators with `@MockitoBean` (or `@MockitoSpyBean` only when interaction spying is needed).
- Use `MockMvc` to assert status and response payload shape.
- Use `JsonMapper` (or established project JSON helper) to serialize request DTOs.

---

## Checklist

- existing resource slice is reused where possible before creating new classes
- controller is thin and contains no business logic or repository access
- request/response models at HTTP boundary are web DTOs
- request validation is applied at DTO/controller boundary
- swagger v3 annotations cover method, request body DTO, and response DTO
- ownership context is derived from trusted server-side authentication context
- mapping is explicit (`Request -> Command/Query`, `Domain -> Response`)
- services and persistence layers are extended only when needed
- touched layers have focused tests, including `@WebMvcTest` for controllers
