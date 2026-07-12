# AGENTS.md

## Agent Working Guidelines.
Ask questions to clarify requirements, constraints, and preferences before starting work. If you encounter ambiguities or edge cases, seek clarification rather than making assumptions. When in doubt, ask for more information.
You must always read relevant copilot instructions and SKILL.md available in the repository before starting work. These documents contain important guidelines, patterns, and known exceptions that are critical for maintaining code quality and consistency.
When implementing features or fixes, follow the established architecture, design patterns, and coding style as outlined in the copilot instructions. This ensures that your contributions align with the overall project structure and maintainability goals.

## Scope
- `.agents/AGENTS.md` is the portable, repo-agnostic guidance for architecture, design, and coding style.
- This file is the source of truth for repository-specific structure, package placement, runtime details, test locations, and known implementation exceptions.

## Snapshot
- Root `pom.xml` currently builds only the `app` module.
- App entry point is `app/src/main/java/com/fabiankevin/app/App.java`.
- The implemented layering is: controllers -> services -> repositories -> jpa_repositories -> entities.

## Package and module layout
- Base package: `com.fabiankevin.app`.
- Web endpoints live in `app/src/main/java/com/fabiankevin/app/web/controllers`.
- Request and response DTOs live in `app/src/main/java/com/fabiankevin/app/web/controllers/dtos`.
- Domain models live in `app/src/main/java/com/fabiankevin/app/models`, with enums under `models/enums`.
- Repository interfaces and implementations live directly under `app/src/main/java/com/fabiankevin/app/persistence`.
- JPA entities live in `app/src/main/java/com/fabiankevin/app/persistence/entities`.
- Spring Data JPA repositories live in `app/src/main/java/com/fabiankevin/app/persistence/jpa_repositories`.
- Services live in `app/src/main/java/com/fabiankevin/app/services`, with supporting types under `services/commands`, `services/queries`, and `services/summaries`.
- Controller tests live in `app/src/test/java/com/fabiankevin/app/web/controllers`.
- Repository tests live in `app/src/test/java/com/fabiankevin/app/persistence`.
- Service and summary tests live in `app/src/test/java/com/fabiankevin/app/services`.

## How the app is structured
- Web endpoints live in `app/src/main/java/com/fabiankevin/app/web/controllers`; request/response DTOs are nested under `web/controllers/dtos`, not `web/dtos`.
- Controllers extract `userId` from `JwtAuthenticationToken.getToken().getSubject()` and pass it into commands; clients never supply `userId` in payloads. See `AccountController` and `TransactionController`.
- Request DTOs convert themselves to service commands (`CreateTransactionRequest#toCommand` -> `AddTransactionCommand`), so new endpoints should keep translation at the web edge.
- Domain models are Java records with `@Builder(toBuilder = true)` and constructor invariants; e.g. `Transaction`, `Account`, and `Amount` reject invalid/null state early.
- Services own orchestration and timestamps (`Instant.now()`), not controllers or repositories; see `DefaultAccountService` and `DefaultTransactionService`.
- Repositories are thin adapters from domain models to Spring Data JPA entities (`DefaultTransactionRepository` -> `JpaTransactionRepository` -> `TransactionEntity`).
- `TransactionService` is wired manually in `app/src/main/java/com/fabiankevin/app/config/AppConfig.java` because `DefaultTransactionService` needs a curated `List<SummaryGenerator>`.
- Summary generation is a strategy map keyed by `SummaryType`; `CategorySummaryGenerator`, `MonthlySummaryGenerator`, `YearlySummaryGenerator`, and `DailySummaryGenerator` are Spring components consumed by `DefaultTransactionService`.
- Known exception: `TransactionService#getTransactionById(...)` currently returns `TransactionResponse`, which leaks a web DTO into the service boundary. Treat this as technical debt to contain rather than a pattern to copy.

## Persistence and data flow
- Entities keep conversion methods both ways (`AccountEntity.from(model)` / `toModel()`); follow that pattern instead of leaking entities into services.
- Cross-user isolation is enforced in service/repository logic, not by request payloads. Examples: `findById(...).filter(a -> a.userId().equals(userId))` and `deleteByIdAndAccountUserId(...)`.
- Transaction summaries are implemented as custom JPQL aggregation queries in `JpaTransactionRepository`; grouped labels are strings/numbers projected through `SummaryPointProjection`.
- Local PostgreSQL schema changes are sourced from Liquibase files under `app/src/main/resources/db`; `db.changelog.master.yml` includes raw SQL rollout/rollback scripts.
- Tests mostly use H2 + `spring.jpa.hibernate.ddl-auto=update`, so test schema is often driven by entities rather than Liquibase.

## Security and API conventions
- API versioning is enabled globally in `app/src/main/resources/application.yaml`; controllers use `@RequestMapping(..., version = "v1")`, with header support via `X-API-VERSION` and default `v1`.
- `ResourceServerConfig` maps JWT `scope` into `SCOPE_*` authorities and JWT `roles` into `ROLE_*`; role hierarchy is `ROLE_ADMIN > ROLE_USER`.
- `/api/accounts/**` and `/api/categories/**` require `ROLE_USER`; everything else falls back to authenticated access. Swagger and actuator health/info/prometheus are public.
- OpenAPI metadata and OAuth2 client-credentials wiring are centralized in `config/OpenApiConfig.java`.

## Build, test, and local run
- Useful commands verified here:
    - `./mvnw -pl app -Dtest=TransactionControllerTest,DefaultTransactionRepositoryTest test`
    - `./mvnw -pl app test`
- Default runtime profile is `local` (`application.yaml`) which uses file-based H2 in `application-local.yaml`.
- For PostgreSQL-backed local work, use `application-local-pg.yaml` plus `docker-compose/docker-compose.yaml`; the compose stack starts Postgres and Adminer, and the init scripts create `financial_tracker_user`/`financial_tracker_apps` plus `financial_tracker_schema`.
- Runtime PG config uses `financial_tracker_apps` (`application-local-pg.yaml`), while Liquibase in `app/pom.xml` uses `financial_tracker_user`; keep that split when changing DB setup.

## Testing patterns to copy
- Controller tests use `@WebMvcTest`, `@MockitoBean`, `MockMvc`, and `jwt()` request post-processors; see `TransactionControllerTest`.
- Repository tests use `@DataJpaTest` plus a nested `@TestConfiguration` to register adapter beans; see `DefaultAccountRepositoryTest` and `DefaultTransactionRepositoryTest`.
- Service tests live under `app/src/test/java/com/fabiankevin/app/services`, and summary strategy tests live under `app/src/test/java/com/fabiankevin/app/services/summaries`.
- Some slice tests run with profile `local`, but `DefaultTransactionRepositoryTest` explicitly uses `@ActiveProfiles("test")`; check each test before assuming the datasource/profile.

