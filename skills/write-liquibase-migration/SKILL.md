# QWEN.md — Identity Service

This file guides the AI assistant during coding sessions. It complements (does not replace) `AGENTS.md`, which holds operational and environmental knowledge. When in doubt, consult both.

---

## Architecture

Light hexagonal architecture with DDD-light modeling. Domain models must be valid by construction — a model should not be creatable in an invalid state.

```
controllers → services → repositories → JPA repositories → entities
```

- **Controllers** are thin web adapters; they delegate to services, never repositories or clients.
- **Services** own orchestration, cross-collaborator validation, and timestamps (`Instant.now()`).
- **Repositories** are thin adapters mapping domain models ↔ JPA entities.
- **Entities** live exclusively inside `persistence/entities/`.

---

## Implementation Rules

### Web layer
- Keep web DTOs close to controllers. Convert domain models → response DTOs here.
- Derive ownership context (`userId`, tenant id) from the authenticated principal or trusted server-side context. **Never accept `userId` from API clients.**
- Translate web DTOs to commands at the edge: request DTOs expose `toCommand(...)`; controllers pass commands to services.

### Service layer
- Service APIs center on commands, queries, and domain models — not persistence types.
- Use `@Service` by default. Use manual `@Configuration` only when bean composition is required (e.g., wiring a curated strategy list with explicit ordering).
- Treat known boundary leaks in existing code as exceptions to contain, not patterns to replicate.

### Persistence layer
- Keep repository adapters thin. Map between domain models and JPA entities using the established `from(model)` / `toModel()` pattern on entities.
- Keep Spring Data JPA interfaces (`Jpa*Repository`) separate from entities and domain-facing repository abstractions.
- Enforce cross-user isolation in service and repository logic, **not** in request payloads.

### Domain models
- Prefer Java records for domain models and immutable carriers.
- Use Lombok `@Builder(toBuilder = true)` where the existing model follows that pattern.
- Use constructor invariants to reject invalid state early.
- Use `Instant` for timestamps, `UUID` for identifiers, and `Optional` — never `null` — where absence is expected.

---

## Coding Standards

### Do
- Follow existing code style and naming conventions.
- Use Java 25 features where they improve clarity without fighting surrounding style.
- Prefer direct imports over wildcard imports.
- Use Lombok selectively to reduce boilerplate in records, DTOs, and Spring components.
- Keep classes and methods focused on a single responsibility.
- Keep OpenAPI, security, and bean wiring changes close to configuration classes rather than scattering across feature packages.
- For pluggable behaviors (summaries, exporters, calculators), prefer a strategy pattern keyed by a clear enum or discriminator type.

### Don't
- Accept `userId` from API clients.
- Leak JPA entities or `Jpa*Repository` types outside the persistence layer.
- Put orchestration timestamps in controllers or repositories.
- Bypass repository/entity conversion methods by leaking entities into services.

---

## Package Structure

```
<root package>
├── <application entry>
├── config
├── exceptions
├── models
│   └── enums
├── persistence
│   ├── <domain-facing repositories>
│   ├── entities
│   └── jpa_repositories
├── services
│   ├── commands
│   ├── queries
│   └── summaries
└── web
    └── controllers
        └── dtos
```

---

## Testing

- **Controller tests**: co-located with the web layer. Use `@WebMvcTest` + `@MockitoBean` + `MockMvc` + authenticated-request pattern when the repo uses Spring MVC slices.
- **Repository tests**: co-located with the persistence layer. Use `@DataJpaTest` + nested `@TestConfiguration` when the repo uses JPA slices.
- **Service/strategy tests**: co-located with the service layer.
- Check the active Spring profile before assuming datasource behavior, schema source, or security configuration.

---

## Related Skills

Concrete skill files live under `.github/skills/`. Reference them when the task matches:

| Skill | Path |
|---|---|
| Test cases | `.github/skills/write-test-cases/SKILL.md` |
| REST API | `.github/skills/write-rest-api/SKILL.md` |
| Domain services | `.github/skills/write-domain-services/SKILL.md` |
| JPA domain repositories | `.github/skills/write-jpa-domain-repositories/SKILL.md` |
| JPA entities | `.github/skills/write-jpa-entities/SKILL.md` |
| Liquibase migration | `.github/skills/write-liquibase-migration/SKILL.md` |
| Spring Cucumber autotests | `.github/skills/write-spring-cucumber-autotests/SKILL.md` |
| B-tree indexes | `.github/skills/write-b-tree-indexes/SKILL.md` |
| Java Optional | `.github/skills/use-java-optional/SKILL.md` |
| Thymeleaf UI / form UX | `.github/skills/thymeleaf-ui-pattern-form-ux/SKILL.md` |
