# AGENTS.md

Personal financial tracker: multi-currency accounts, transactions, categories, aggregated statistics, and shared-spaces collaboration. Spring Boot 3 + Spring Security OAuth2 resource server, backed by PostgreSQL (local H2 for tests).

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

**Shared spaces** — Multi-user collaboration via invitations, participants, and sharing rules. Domain models live in `models/shared_space/`; entities in `persistence/entities/` (`SharedSpaceEntity`, `SpaceParticipantEntity`, `SharedResourceEntity`, `InvitationEntity`, `SharingRuleEmbeddable`). Permission resolution is encapsulated in `SharingPermissionResolver`.

**User provisioning** — `UserProvisioningService` coordinates onboarding (`DefaultUserProvisioningService` splits work into `UserAccountProvisioner` and `UserCategoryProvisioner`, with in-memory implementations available).

---

## Controllers

| Controller | Path |
|-----------|------|
| `AccountController` | `/api/accounts` |
| `CategoryController` | `/api/categories` |
| `TransactionController` | `/api/transactions` |
| `StatsController` | `/api/stats` |
| `UserCreatedEventController` | `/api/users` |

Request/response DTOs are nested under `web/controllers/dtos/` (e.g. `CreateTransactionRequest`, `PageResponse`, `StatsQuery`).

---

## Domain models

`models/` holds immutable records with `@Builder(toBuilder = true)` and constructor invariants. Timestamps are `Instant`, identifiers are `UUID`, absence is `Optional` (not `null`).

Key models: `Account`, `Transaction`, `Amount`, `Category`, `User`, `Page`, `AccountSummary`, `CategorySummary`, `SummaryPoint`, `SummarySeries`, `StatsSummary`.

Shared-space models (`models/shared_space/`): `SharedSpace`, `Invitation`, `SpaceParticipant`, `SharedResource`, `SharingRule`, `SharingPermissionResolver`.

Enums (`models/enums/`): `SummaryType`, `TransactionType`, `AccountType`, `AccountStatus`, `Category`, `UserStatus`, `InvitationStatus`, `ParticipantStatus`, `AccessLevel`, `JointSpaceType`, `ResourceType`, `SharingMode`.

---

## Persistence

- Repository interfaces live in `persistence/` alongside their `Default*` implementations. JPA interfaces (`Jpa*Repository`) sit in `jpa_repositories/`; entities in `entities/`.
- Model/entity conversion is bidirectional on entities: `Entity.from(model)` and `entity.toModel()`. Never leak entities into services.
- JPQL projections (`AccountSummaryProjection`, `CategorySummaryProjection`, `SummaryPointProjection`) live in `persistence/entities/projections/`.
- Schema via Liquibase under `src/main/resources/db/`; master changelog includes raw SQL scripts.

---

## Security & API

- API versioning via `spring.mvc.apiversion` with `X-API-VERSION` header; default `v1`.
- `ResourceServerConfig` maps JWT `scope` → raw authority (e.g. `WRITE`) and JWT `roles` → raw authority (e.g. `USER`, `ADMIN`). No auto-`ROLE_` prefix — token claim values are used verbatim.
- Role hierarchy: `ADMIN > USER`.
- Protected endpoints require `USER`: `/api/accounts/**`, `/api/categories/**`, `/api/stats`, `/api/stats*`.
- User provisioning (`POST /api/users/**`) requires authority `user:provision`.
- Public: `/actuator/health`, `/actuator/info`, `/actuator/prometheus**`, `/swagger-ui/**`, `/v3/api-docs/**`.
- OAuth2 client-credentials for downstream REST calls configured in `OpenApiConfig`.
- Error handling: `BearerAccessDeniedHandler`, `InvalidTokenAuthenticationEntryPoint`.

---

## Build & test

| Command | Purpose |
|---------|---------|
| `./mvnw -pl app test` | Full app test suite |
| `./mvnw -pl app -Dtest=StatsControllerTest,DefaultTransactionRepositoryTest test` | Targeted tests |

- Default profile `local` → file-based H2 (`application-local.yaml`).
- PostgreSQL: `application-local-pg.yaml` + `docker-compose/docker-compose.yaml`.
- Runtime uses `financial_tracker_apps`; Liquibase uses `financial_tracker_user`.

### Testing conventions

- Controller: `@WebMvcTest` + `@MockitoBean` + `MockMvc` + `jwt()` post-processor (see `StatsControllerTest`).
- Repository: `@DataJpaTest` + nested `@TestConfiguration` (see `DefaultTransactionRepositoryTest`). Some slices use `local`; repository tests typically use `@ActiveProfiles("test")` — verify per test.
- Service/strategy tests live next to tested class under `app/src/test/java/.../services/`.
- Cache config: `CacheConfig`.

---

## Known exceptions

- `TransactionService#getTransactionById(...)` returns `TransactionResponse` (a web DTO leak across the service boundary). Contain; don't copy.
