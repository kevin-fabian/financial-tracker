# Project Architecture Guidelines

## Development Workflow

1. Inspect the existing implementation.
2. Identify the affected layers and relevant skills.
3. Read applicable architecture documentation.
4. Implement the smallest complete solution.
5. Add or update tests.
6. Run relevant verification commands.
7. Review the final changes.

## Project Conventions

- Java 25 and Spring Boot 4.x.x.
- Follow the existing hexagonal architecture.
- Keep business logic separate from infrastructure concerns.
- Follow existing naming conventions and patterns.

## Project Architecture Overview

This project follows light hexagonal architecture principles with DDD-light modeling. Keep domain models valid by construction: a model should not exist if it cannot be in a valid state.

**Architecture reference**
controllers -> services -> repositories -> jpa_repositories -> entities

This file is intentionally repo agnostic. For concrete package names, module layout, infrastructure choices, profiles, and known exceptions in the current repository, consult `AGENTS.md`.

---

## Available Skills

Use the relevant skills based on the task.

| Skill | When to use |
|---|---|
| **write-rest-api-controller** | Implementing or modifying REST endpoints — thin controllers, validated web DTOs, command/query mapping |
| **write-pagination-rest-api** | Adding paginated endpoints with `PageQuery`, `Page`, and `PageResponse` |
| **write-domain-models** | Creating or modifying domain model records — builder pattern, constructor invariants, static factories |
| **write-domain-services** | Implementing business rules and workflows — interface-first design, domain-only contracts |
| **write-exceptions** | Writing domain-driven exceptions that name the broken rule and carry data for error responses |
| **write-jpa-entities** | Writing JPA entities with correct mapping, ownership, and fetch strategies |
| **write-jpa-domain-repositories** | Implementing Spring Data `JpaRepository` plus framework-independent domain repository adapters |
| **write-jpa-projection-queries** | Writing JPA projection queries using lightweight DTOs with primitive types and `COALESCE` |
| **write-oauth2-rest-client** | Calling protected downstream services with automatic `client_credentials` token acquisition |
| **write-liquibase-migrations** | Creating database migrations with Liquibase (raw SQL scripts, master changelog) |
| **write-test-cases** | Writing clear test cases across all layers — controller, service, repository |
| **write-springboot-controller-test** | Full `@SpringBootTest` controller tests with JWT auth, `@MockitoBean` overrides, test data helpers |
| **write-springboot-integration-test** | Full `@SpringBootTest` integration tests exercising the complete request pipeline |
| **write-spring-cucumber-autotests** | Cucumber-based integration tests with Allure reporting |
| **write-batch-streaming-job** | Memory-efficient batch jobs that stream records, process in batches, and bulk-insert results |
| **write-b-tree-indexes** | Designing effective B-tree indexes for equality, range, and join queries |
| **service-level-architecture** | Analyzing business capabilities, reviewing architecture consistency before implementation |
| **comply-to-light-hexagonal-architecture** | Deciding where a class belongs — domain models stay framework-agnostic, infrastructure in adapters |
| **modern-java-best-practices** | Applying modern Java idioms — records, sealed types, pattern matching, text blocks, switch expressions |
| **thymeleaf-ui-pattern-form-ux** | Thymeleaf templates, Tailwind styling, form binding, validation patterns |
| **use-java-optional** | **Mandatory** — Replace manual null checks with `Optional` |
| **use-imported-classes** | **Mandatory** — Use direct imports instead of fully qualified class names |

Mandatory skills activate automatically. The rest activate based on task relevance.

---

## General Implementation Rules

### Web layer
- Keep controllers in the web layer and keep web DTOs close to controllers.
- Derive `userId`, tenant id, or similar ownership context from the authenticated principal or another trusted server-side context; clients must never send it in request payloads.
- Keep DTO-to-command translation at the web edge. For example, request DTOs should expose `toCommand(...)` methods and controllers should pass commands to services.
- Prefer converting domain models to response DTOs in controllers.

### Service layer
- Keep service APIs centered on commands, queries, and domain models.
- Services own orchestration, validation that spans multiple collaborators, and timestamps such as `Instant.now()`.
- Use standard Spring stereotypes such as `@Service` by default.
- Use manual configuration only when bean composition is required, such as wiring a curated strategy list or another explicitly ordered dependency set.
- If a repository already contains a known boundary leak, treat it as an exception to contain rather than a pattern to copy into new APIs.

### Persistence layer
- Keep repository adapters thin and map between domain models and JPA entities.
- Follow the existing conversion pattern on entities such as `from(model)` and `toModel()`.
- Keep Spring Data JPA interfaces separate from entities and domain-facing repository abstractions.
- Enforce cross-user isolation in service and repository logic, not in request payloads.

### Domain models
- Prefer Java records for domain models and immutable carriers.
- Use Lombok `@Builder(toBuilder = true)` where the existing model follows that pattern. Do not chain builder calls on a single line — spread them across multiple lines:

```java
// Bad — single line
User.builder().id(id).name("john").build();

// Good — multi line
User.builder()
    .id(id)
    .name("john")
    .build();
```
- Use constructor invariants to reject invalid state early.
- Use `Instant` for timestamps, `UUID` for identifiers, and `Optional` instead of returning `null` where absence is expected.

---

## Coding Standards and Best Practices

### Do
- Follow existing code style and naming conventions in the project.
- Use modern Java (17+) idioms — records, sealed types, pattern matching, switch expressions, streams, and method references — when they improve clarity.
- Use direct imports instead of fully qualified class names in source code.
- Use Lombok selectively to reduce boilerplate in records, DTOs, and Spring components.
- Keep classes and methods focused on a single responsibility; extract reusable logic only when it genuinely has multiple callers.
- Keep OpenAPI, security, and bean wiring changes close to configuration classes rather than scattering them across feature packages.
- For pluggable behaviors such as summaries, exporters, or calculators, prefer a strategy pattern keyed by a clear enum or discriminator type.
- Return early for invalid or terminal conditions using guard clauses to reduce nesting.
- Use loops instead of streams when a loop is simpler, easier to debug, or better suited to stateful logic.
- Handle absence and errors explicitly — do not call `Optional.get()` without checking; do not silently swallow exceptions.

### Don't
- Don't accept `userId` from API clients.
- Don't leak JPA entities or `Jpa*Repository` types outside the persistence layer.
- Don't put orchestration timestamps in controllers or repositories.
- Don't bypass repository/entity conversion methods by leaking entities into services.
- Don't assume packages, modules, or infrastructure exist without verifying the current repository.

---