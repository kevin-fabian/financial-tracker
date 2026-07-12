# Project Architecture Guidelines

## Project Architecture Overview
This project follows light hexagonal architecture principles with DDD-light modeling. Keep domain models valid by construction: a model should not exist if it cannot be in a valid state.

**Architecture reference**
controllers -> services -> repositories -> jpa_repositories -> entities

This file is intentionally repo agnostic. For concrete package names, module layout, infrastructure choices, profiles, and known exceptions in the current repository, consult `AGENTS.md`.

---

## Related Repository Skills

- [Test Case Skill](.github/skills/write-test-cases/SKILL.md)
- [REST API Skill](.github/skills/write-rest-api/SKILL.md)
- [Domain Service Skill](.github/skills/write-domain-services/SKILL.md)
- [JPA + Domain Repository Skill](.github/skills/write-jpa-domain-repositories/SKILL.md)
- [JPA Entity Skill](.github/skills/write-jpa-entities/SKILL.md)


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
- Use Lombok `@Builder(toBuilder = true)` where the existing model follows that pattern.
- Use constructor invariants to reject invalid state early.
- Use `Instant` for timestamps, `UUID` for identifiers, and `Optional` instead of returning `null` where absence is expected.

---

## Coding Standards and Best Practices

### Do
- Follow existing code style and naming conventions in the project.
- Use Java 25 language features where they improve clarity without fighting the surrounding code style.
- Prefer direct imports over wildcard imports.
- Use Lombok selectively to reduce boilerplate in records, DTOs, and Spring components.
- Keep classes and methods focused on a single responsibility.
- Keep OpenAPI, security, and bean wiring changes close to configuration classes rather than scattering them across feature packages.
- For pluggable behaviors such as summaries, exporters, or calculators, prefer a strategy pattern keyed by a clear enum or discriminator type.

### Don’t
- Don’t accept `userId` from API clients.
- Don’t leak JPA entities or `Jpa*Repository` types outside the persistence layer.
- Don’t put orchestration timestamps in controllers or repositories.
- Don’t bypass repository/entity conversion methods by leaking entities into services.
- Don’t assume packages, modules, or infrastructure exist without verifying the current repository.

---

## Package Structure Standards

```bash
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

## Testing and Profiles

- Put controller tests with the web layer and follow the established `@WebMvcTest` + `@MockitoBean` + `MockMvc` + authenticated-request pattern when the repository uses Spring MVC slices.
- Put repository tests with the persistence layer and follow the established `@DataJpaTest` + nested `@TestConfiguration` pattern when the repository uses JPA slices.
- Keep service and strategy tests with the service layer.
- Check the active profile before assuming datasource behavior, schema source, or security configuration.
