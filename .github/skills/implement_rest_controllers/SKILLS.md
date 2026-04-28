---
name: rest-api-controller
description: 'Implement Spring REST endpoint slices with validated request and response DTOs, service-layer mapping, optional persistence changes, and focused tests. Use when asked to add or update REST endpoints, controller DTOs, validation, or controller tests.'
argument-hint: 'What resource or endpoint is being implemented? Include request and response JSON or existing Java record models.'
---

# REST API Controller

Use this skill to implement or update a Spring REST endpoint in a project that follows the shared layered architecture.

Before applying concrete paths, package names, naming patterns, or runtime conventions:

- use `.github/copilot-instructions.md` for the portable architecture and design rules
- use `AGENTS.md` for the current repository's actual layout, infrastructure, testing conventions, and known exceptions

## Clarify Up Front

Confirm or infer the following before making changes:

- [ ] endpoint behavior
- [ ] request/response JSON or existing models
- [ ] auth and ownership context (for example `userId`, tenant id, or organization scope) and where it must come from
- [ ] expected status codes and error behavior
- [ ] which tests are in scope
- [ ] whether an existing resource slice already covers the resource

---

## Quick REST API Endpoint Flow Reference

**Typical steps** Controller → Service → Repository

Use the shared project structure as a reference, then verify the concrete local layout before creating files:

```text
<base-package>
├── web
│   └── controllers
│       ├── *Controller.java              <-- HTTP mapping, auth/context, DTO mapping
│       └── dtos/
│           ├── *Request.java             <-- validated request DTOs
│           ├── *Response.java            <-- HTTP response DTOs
│           └── <shared HTTP DTOs when needed>
├── services
│   ├── *Service.java                     <-- service interface
│   ├── <service implementation>
│   ├── commands/
│   │   └── *Command.java
│   └── queries/
│       └── *Query.java
└── persistence
    ├── *Repository.java                  <-- domain-facing repository interface
    ├── <repository implementation>
    ├── jpa_repositories/
    │   └── Jpa*Repository.java           <-- Spring Data repository
    └── entities/
        └── *Entity.java                  <-- persistence entity
```

Typical flow:

`*Request` → `*Controller` → `*Service` → `<service implementation>` → `*Repository` → `<repository implementation>` → `Jpa*Repository` → `*Entity`

Not every endpoint change needs every layer. Extend lower layers only when the existing slice cannot support the requested behavior.

## Reuse Existing Resource Slice Before Creating New Classes

Before adding classes, scan the existing resource slice first.

- Extend an existing `*Controller` before creating another controller for the same resource.
- Extend an existing `*Service` and its implementation before creating another service.
- Extend an existing repository chain before creating another repository slice.
- Reuse DTOs, commands, queries, and mappers when the contract is the same or close.
- Create a new slice only when the resource does not exist.

## Workflow

1. Scan the existing resource slice in `web`, `services`, and `persistence`.
   Reuse what exists before creating new classes.
2. Confirm the HTTP contract, ownership rules, and whether the change fits the existing `controller -> service -> repository` slice.
3. Add or update request and response DTOs close to the controller layer used by the repository.
   Keep request and response models at the HTTP boundary.
4. Add boundary validation with `jakarta.validation` and `@Valid`.
5. Implement or extend the controller.
   Keep it thin: map HTTP input, extract auth/context, call the service, map the response.
   See [REST controller instructions](../../instructions/web/controllers/REST_CONTROLLER.instructions.md).
6. Map controller input to service commands or queries.
   Keep business logic in the service layer.
7. Implement or extend the service interface and service implementation.
   Services own business rules, orchestration, and transactions.
   See [service instructions](../../instructions/services/SERVICE.instructions.md).
8. If needed, extend the persistence slice from the domain-facing repository through the persistence implementation to Spring Data and entities.
   Keep persistence mapping inside the persistence layer.
   See [repository instructions](../../instructions/persistence/repositories/REPOSITORY.instructions.md), [JPA repository instructions](../../instructions/persistence/jpa_repositories/JPA_REPOSITORY.instructions.md), and [entity instructions](../../instructions/persistence/entities/ENTITY.instructions.md).
9. Add focused tests for the touched layers that are in scope and run them.
   See [controller test instructions](../../instructions/web/controllers/TEST_REST_CONTROLLER.instructions.md), [service test instructions](../../instructions/services/SERVICE_TEST.instructions.md), [repository test instructions](../../instructions/persistence/repositories/REPOSITORY_TEST.instructions.md), and [general test instructions](../../instructions/tests/TEST.instructions.md).

## Completion Checks

- the controller uses `@RestController` and request mapping annotations
- request and response types at the HTTP boundary are web DTOs
- validation stays at the request DTO and controller boundary
- ownership context is derived from trusted server-side context, not client payloads
- DTO-to-command and domain-to-response mapping is explicit
- the controller contains no business logic or repository access
- tests cover the touched layers that were changed and are in scope

## Example Prompts

- Implement a create endpoint from request and response JSON.
- Add a `GET /api/resources/{id}` endpoint using existing models.
- Refactor a controller so request DTOs map to service commands and response DTOs map from domain models.
- Implement an endpoint and add focused controller tests.
