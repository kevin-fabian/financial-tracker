---
name: rest-api-controller
description: 'Implement Spring REST controllers with validated request and response DTOs, service-layer mapping, and focused tests. Use when asked to add or update REST endpoints, controller DTOs, validation, or controller tests.'
argument-hint: 'What resource or endpoint is being implemented? Include request and response JSON or existing Java record models.'
---

# REST API Controller

Use this skill to implement or update a Spring REST endpoint in this repository.

## Ask First

Ask the user for:

- the resource and endpoint behavior
- request JSON, response JSON, or existing Java record models
- whether the task also includes controller, service, or repository tests

## Workflow

1. Confirm the HTTP contract from the user's JSON or Java records. Start with [repository architecture](../../copilot-instructions.md).
2. Define or update web request and response DTOs for the endpoint. Follow [REST controller instructions](../../instrunctions/web/controllers/REST_CONTROLLER.instructions.md).
3. Add boundary validation with `jakarta.validation` on request DTOs and `@Valid` in the controller. Follow [REST controller instructions](../../instrunctions/web/controllers/REST_CONTROLLER.instructions.md).
4. Implement the controller endpoint, keep it limited to HTTP mapping, and delegate through a service interface. Follow [REST controller instructions](../../instrunctions/web/controllers/REST_CONTROLLER.instructions.md).
5. Map request DTOs to service command or query objects before calling the service. Follow [service instructions](../../instrunctions/services/SERVICE.instructions.md).
6. If the service needs persistence changes, implement that slice with [repository instructions](../../instrunctions/persistence/repositories/REPOSITORY.instructions.md) and [JPA repository instructions](../../instrunctions/persistence/jpa_repositories/JPA_REPOSITORY.instructions.md).
7. Map the service result to a web response DTO. Return web models, not domain models or entities. Follow [REST controller instructions](../../instrunctions/web/controllers/REST_CONTROLLER.instructions.md).
8. Add endpoint and schema documentation where the repository expects OpenAPI annotations. Follow [REST controller instructions](../../instrunctions/web/controllers/REST_CONTROLLER.instructions.md).
9. Add focused tests for each touched layer: [controller tests](../../instrunctions/web/controllers/TEST_REST_CONTROLLER.instructions.md), [service tests](../../instrunctions/services/SERVICE_TEST.instructions.md), [repository tests](../../instrunctions/persistence/repositories/REPOSITORY_TEST.instructions.md), and [general test conventions](../../instrunctions/tests/TEST.instructions.md).
10. Finish by running the relevant tests for the layers you changed.

## Completion Checks

- the controller uses `@RestController` and request mapping annotations
- request and response types at the HTTP boundary are web DTOs
- validation stays at the request DTO and controller boundary
- DTO-to-command and domain-to-response mapping is explicit
- the controller contains no business logic or repository access
- tests cover each touched layer

## Example Prompts

- Implement a `UserController` create endpoint from request and response JSON.
- Add a `GET /api/users/{id}` endpoint using existing Java record models.
- Refactor a controller so request DTOs map to service commands and response DTOs map from domain models.
- Implement a controller and add focused `@WebMvcTest` coverage.
