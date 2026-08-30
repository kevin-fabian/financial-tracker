## IntelliJ MCP Tools

Always prefer IntelliJ MCP over grep/regex/file tools.

```
# Search: symbol
tool: mcp__intellij__search_symbol(q="PasswordChangeService")

# Navigate: symbol info, call hierarchy
tool: mcp__intellij__get_symbol_info(filePath="app/src/.../File.java", line=5, column=1)
tool: mcp__intellij__analyze_calls(symbolFqn="com.fabiankevin.app.services.DefaultUserService.create", analysisKind="INCOMING_CALLS")

# Analyze: file problems, lint
tool: mcp__intellij__get_file_problems(filePath="app/src/.../File.java")
tool: mcp__intellij__lint_files(files=["app/src/.../File.java"])

# Edit: rename, format
tool: mcp__intellij__rename_refactoring(pathInProject="app/src/.../File.java", symbolName="OldName", newName="NewName")
tool: mcp__intellij__reformat_file(files=["app/src/.../File.java"])
```

Personal financial tracker: multi-currency accounts, transactions, categories, aggregated statistics, budgets, recurring transactions, shopping lists, and party collaboration. Spring Boot 3 + Spring Security OAuth2 resource server, backed by PostgreSQL (local H2 for tests).

Entry point: `app/src/main/java/com/fabiankevin/app/App.java`. Single-module build (`app`); multi-module parent is scaffolding for future use.

---

## Architecture

Layered hexagonal: `controllers -> services -> repositories -> jpa_repositories -> entities`. Domain models (`models/`, Java records) cross layer boundaries; JPA entities never escape `persistence/`.

### Key patterns

**Commands & queries at the web edge** — Request DTOs convert themselves to service commands/queries in the controller (`CreateTransactionRequest#toCommand`, `StatsQuery`, `PageRequest`).

**User context from JWT** — Controllers extract `userId` from `JwtAuthenticationToken.getToken().getSubject()`. Clients must never send ownership identifiers in payloads; cross-user isolation is enforced in service/repository logic.

**Summary strategies** — `SummaryType` selects a `SummaryGenerator` implementation (`Daily`, `Monthly`, `Yearly`, `Category`). Curated list is wired manually in `AppConfig` (see `DefaultTransactionService`).

**Caching decorators** — Read-heavy services are wrapped by a `Cached*Service` variant (currently `CachedTransactionService`, `CachedCategoryService`; `CachedAccountService` is intentionally commented out — don't re-enable without a reason). Injected as the default bean for queries.

**Stats aggregation** — `StatsService` builds `StatsSummary` from JPQL projections in `persistence/entities/projections/`. Query params arrive through `StatsQuery`.

**Party collaboration** — Multi-user collaboration via parties, invitations, members, and shared items. Domain models live in `models/party/`; entities in `persistence/entities/` (`PartyEntity`, `PartyMemberEntity`, `SharedItemEntity`, `InvitationEntity`). Parties are owned by a `partyLeaderId` and governed by a `SharingMode`.

**User provisioning** — `UserProvisioningService` coordinates onboarding (`DefaultUserProvisioningService` splits work into `UserAccountProvisioner` and `UserCategoryProvisioner`, with in-memory implementations available).

**Event publishing** — `events/` package provides `EventPublisher` implementations (`TransactionEventPublisher`, `StatsEventPublisher`) composed via `CompositeTransactionEventPublisher`. `ItemEvent<T>` is the generic event record.

**Downstream user enrichment** — `clients/` package provides `UserClient` (`DefaultUserClient`) for fetching user details (name, initials) from a downstream service.

---

## Controllers

| Controller | Path |
|-----------|------|
| `AccountController` | `/api/accounts` |
| `CategoryController` | `/api/categories` |
| `TransactionController` | `/api/transactions` |
| `StatsController` | `/api/stats` |
| `BudgetController` | `/api/budgets` |
| `RecurringTransactionController` | `/api/recurring-transactions` |
| `PartyController` | `/api/parties` |
| `InvitationController` | `/api/parties/invitations` |
| `UserCreatedEventController` | `/api/users` |

Request/response DTOs are nested under `web/controllers/dtos/` (e.g. `CreateTransactionRequest`, `PageResponse`, `StatsQuery`, `party/PartyResponse`, `budgets/BudgetResponse`).

---

## Domain models

`models/` holds immutable records with `@Builder(toBuilder = true)` and constructor invariants. Timestamps are `Instant`, identifiers are `UUID`, absence is `Optional` (not `null`).

Key models: `Account`, `Transaction`, `Amount`, `Category`, `User`, `Page`, `AccountSummary`, `CategorySummary`, `SummaryPoint`, `SummarySeries`, `StatsSummary`, `ItemEvent<T>`.

**Party models** (`models/party/`): `Party`, `PartyMember`, `Invitation`, `SharedItem`, `PartySummary`, `PartyMemberSummary`, `InvitationSummary`.

**Budget models** (`models/budgets/`): `Budget`, `BudgetSummary`, `BudgetPeriod` (enum).

**Recurring transaction models** (`models/recurring_transactions/`): `RecurringTransaction`, `RecurringTransactionSummary`, `RecurringTransactionStatus`, `TransactionStatus`.

**Shopping list models** (`models/shopping_list/`): `ShoppingList`, `ShoppingItem`.

**Enums** (`models/enums/`): `SummaryType`, `TransactionType`, `AccountType`, `AccountStatus`, `Category`, `UserStatus`, `EventAction`, `ItemPriority`, `ShoppingListStatus`.

**Party enums** (`models/enums/party/`): `AccessLevel`, `InvitationStatus`, `PartyMemberStatus`, `ResourceType`, `SharingMode`.

---

## Persistence

- Repository interfaces live in `persistence/` alongside their `Default*` implementations. JPA interfaces (`Jpa*Repository`) sit in `jpa_repositories/`; entities in `entities/`.
- Model/entity conversion is bidirectional on entities: `Entity.from(model)` and `entity.toModel()`. Never leak entities into services.
- JPQL projections (`AccountSummaryProjection`, `CategorySummaryProjection`, `SummaryPointProjection`, `BudgetSummaryProjection`, `RecurringTransactionSummaryProjection`) live in `persistence/entities/projections/`.
- Schema via Liquibase under `src/main/resources/db/`; master changelog (`db.changelog.master.yml`) includes raw SQL scripts (`rollouts/`, `rollbacks/`).

### Party entities
- `PartyEntity` (`parties` table) — owns `PartyMemberEntity` and `SharedItemEntity` via `OneToMany` with `orphanRemoval`.
- `PartyMemberEntity` (`party_members` table) — `playerId`, `accessLevel`, `status`, `joinedAt`.
- `SharedItemEntity` (`shared_items` table) — `resourceType`, `itemIds` (JSON column), `sharedAt`.
- `InvitationEntity` (`invitations` table) — `inviterPlayerId`, `inviteePlayerId`, `proposedSharingMode`, `proposedRole`, `status`, `expiresAt`.

---

## Security & API

- API versioning via `spring.mvc.apiversion` with `X-API-VERSION` header; default `v1`.
- `Oauth2ResourceServerConfig` maps JWT `scope` → raw authority (e.g. `WRITE`) and JWT `roles` → raw authority (e.g. `USER`, `ADMIN`). No auto-`ROLE_` prefix — token claim values are used verbatim.
- Role hierarchy: `ADMIN > USER`.
- Protected endpoints require `USER`: `/api/accounts/**`, `/api/categories/**`, `/api/stats`, `/api/stats*`, `/api/budgets/**`, `/api/recurring-transactions/**`, `/api/parties/**`.
- User provisioning (`POST /api/users/**`) requires authority `user:provision`.
- Public: `/actuator/health`, `/actuator/info`, `/actuator/prometheus**`, `/swagger-ui/**`, `/v3/api-docs/**`.
- OAuth2 client-credentials for downstream REST calls configured in `Oauth2RestClientConfig` and `OpenApiConfig`.
- Error handling: `BearerAccessDeniedHandler`, `InvalidTokenAuthenticationEntryPoint`.

---

## Build & test

### Run Tests
```bash
# Full app test suit
./mvnw -pl app test 2>&1 | grep -E "Tests run:|BUILD"

# Targeted tests:
./mvnw -pl app test -Dtest=CategoryControllerSpringBootTest 2>&1 | grep -E "Tests run:|BUILD"
```
- Default profile `local` → file-based H2 (`application-local.yaml`).
- PostgreSQL: `application-local-pg.yaml` + `docker-compose/docker-compose.yaml`.
- Runtime uses `financial_tracker_apps`; Liquibase uses `financial_tracker_user`.

### Testing conventions

- Controller: `@WebMvcTest` + `@MockitoBean` + `MockMvc` + `jwt()` post-processor (see `StatsControllerTest`). Some controllers use full `@SpringBootTest` (see `PartyControllerTest`, `BudgetControllerSpringBootTest`).
- Repository: `@DataJpaTest` + nested `@TestConfiguration` (see `DefaultTransactionRepositoryTest`). Some slices use `local`; repository tests typically use `@ActiveProfiles("test")` — verify per test.
- Service/strategy tests live next to tested class under `app/src/test/java/.../services/`.
- Cache config: `CacheConfig`.

---

## Known exceptions

- `TransactionService#getTransactionById(...)` returns `TransactionResponse` (a web DTO leak across the service boundary). Contain; don't copy.
