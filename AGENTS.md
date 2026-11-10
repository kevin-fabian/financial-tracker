# Project Architecture Guidelines

## Development Workflow

Follow these steps for every implementation task.

1. **Inspect**
    - Read the relevant source files, tests, and project instructions.
    - Check existing patterns before introducing new abstractions.
    - Identify affected layers, dependencies, and behavior.

2. **Select skills**
    - Load the mandatory skills for the task.
    - Load additional skills only when their guidance applies.
    - Follow each skill's instructions on when to consult its references.

3. **Plan**
    - Identify the intended behavior and affected components.
    - Choose the simplest solution consistent with the architecture.
    - Identify necessary tests and generated files.

4. **Implement**
    - Keep changes scoped to the requested behavior.
    - Reuse existing components, providers, repositories, and utilities.
    - Update related tests and documentation when necessary.

5. **Verify**
    - Run IntelliJ Lint on all modified files and fix any findings.
    - Run targeted tests for modified and new classes.
    - Run broader tests when practical.
    - Run `use-java-optional` and `use-imported-classes` for modified and new classes.

6. **Review**
    - Check architectural boundaries, error handling, and edge cases.
    - Inspect the final diff for unrelated changes.
    - Report completed work, verification results, and any remaining issues.

## Project Conventions

- Java 25 and Spring Boot 4.x.x.
- Follow the existing hexagonal architecture.
- Keep business logic separate from infrastructure concerns.
- Follow existing naming conventions and patterns.

## Project Architecture Overview

This project follows light hexagonal architecture principles with DDD-light modeling. Keep domain models valid by construction: a model should not exist if it cannot be in a valid state.

**Architecture reference**
controllers -> services -> repositories -> jpa_repositories -> entities

**Package structure** (feature-based modules)
```
com.fabiankevin.app.<feature>/
├── web/                    # REST controllers and DTOs
├── domain/
│   ├── models/             # Domain entities and enums
│   ├── repositories/       # Domain repository interfaces
│   ├── services/           # Service interfaces + implementations (commands/queries subfolders)
│   └── exceptions/         # Domain-driven exceptions that name the broken business rule
└── persistence/
    ├── entities/           # JPA entities
    ├── Jpa*Repository.java # Spring Data JPA repositories
    └── Default*Repository.java # Repository adapters
```

This file is intentionally repo agnostic. For concrete package names, module layout, infrastructure choices, profiles, and known exceptions in the current repository, consult `AGENTS.md`.

---

## Mandatory Skills

These skills activate automatically on every turn:

| Skill | Rule |
|---|---|
| **use-java-optional** | Replace manual null checks with `Optional` |
| **use-imported-classes** | Use direct imports instead of fully qualified class names |
| **modern-java-best-practices** | Apply modern Java idioms — records, sealed types, pattern matching, text blocks, switch expressions |

---

## Available Skills

Use the relevant skills based on the task.

| Skill | When to use |
|---|---|
| **write-test-cases** | Writing clear test cases across all layers — controller, service, repository |
| **write-rest-api-controller** | Implementing or modifying REST endpoints — thin controllers, validated web DTOs, command/query mapping |
| **write-domain-models** | Creating or modifying domain model records — builder pattern, constructor invariants, static factories |
| **write-domain-services** | Implementing business rules and workflows — interface-first design, domain-only contracts |
| **write-exceptions** | Writing domain-driven exceptions that name the broken rule and carry data for error responses |
| **write-jpa-entities** | Writing JPA entities with correct mapping, ownership, and fetch strategies |
| **write-jpa-domain-repositories** | Implementing Spring Data `JpaRepository` plus framework-independent domain repository adapters |
| **write-jpa-projection-queries** | Writing JPA projection queries using lightweight DTOs with primitive types and `COALESCE` |
| **write-liquibase-migrations** | Creating database migrations with Liquibase (raw SQL scripts, master changelog) |
| **write-b-tree-indexes** | Designing effective B-tree indexes for equality, range, and join queries |

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

## Verification Checklist

Before marking work complete, confirm:

- [ ] All test cases are passing
- [ ] Code complies with `use-imported-classes` (direct imports, no fully qualified names)
- [ ] Code complies with `use-java-optional` (no manual null checks, use `Optional`)
- [ ] Code complies with `modern-java-best-practices` (records, sealed types, pattern matching, text blocks, switch expressions)
- [ ] IntelliJ Lint passes on all modified files (mandatory — run `lint_files` before marking work complete)