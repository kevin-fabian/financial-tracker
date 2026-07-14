---
name: write-domain-services
description: Workflow and best practices for writing framework-light domain services with interface-first design and domain-only contracts.
---

# Domain Service Best-Practice Skill

Primary goal: write domain services that are interface-first, framework-light, and strictly aligned with domain models and domain exceptions.

References:
- `.github/skills/write-jpa-domain-repositories/SKILL.md`
- `.github/skills/write-test-cases/SKILL.md`

## Workflow

1. Define the domain service interface first (for example `UserService`).
2. Define method contracts using `Command` / `Query` objects, or normal parameters only when the total parameter count is less than 3.
3. Implement the interface with `Default...Service` (for example `DefaultUserService`).
4. Keep service contracts and behavior domain-centric: inputs are command/query/primitive values, outputs are domain models.
5. Orchestrate domain repositories and related domain services to enforce business rules.
6. Expose and handle domain exceptions only; downstream layers must already translate infrastructure exceptions into domain exceptions.

---

## Mandatory Rules

- Always start with interface then implementation: `UserService` -> `DefaultUserService`.
- Service methods must return domain models (for example `User`, `Order`, `Item`, `Set<User>`, `Optional<User>`), never framework entities or HTTP objects.
- Service method parameters must follow:
  - use command objects for write use cases (`CreateUserCommand`, `PatchUserCommand`)
  - use query objects for search/list/pagination (`PageQuery`, `UserQuery`)
  - use normal parameters (`String`, `UUID`, `int`, `double`, etc.) only when parameter count is less than 3
- Do not depend on framework classes in service contracts or business logic; annotation usage is allowed (`@Service`, Lombok annotations, stereotype annotations).
- Domain services must only throw/propagate domain exceptions.
- Infrastructure/framework exceptions must be translated in downstream adapters/repositories before reaching domain services.
- Keep business validation and invariants in the service (or domain model) rather than controller/infrastructure layers.
- Use `@Transactional` on write operations that span multiple persistence calls or side effects.

---

## Practical: Step 1 - Define the interface first

```java
public interface UserService {
    User create(CreateUserCommand command);
    User patch(PatchUserCommand command);
    User getById(UUID userId);
    Set<User> find(PageQuery query);
}
```

Guidance:
- Keep this interface free from Spring/framework types.
- Return only domain models.

---

## Practical: Step 2 - Define Command and Query contracts

```java
@Builder(toBuilder = true)
public record CreateUserCommand(String name, String email) {}

@Builder(toBuilder = true)
public record PatchUserCommand(UUID id, String name, String email) {}

@Builder(toBuilder = true)
public record PageQuery(int page, int size) {}
```

Example for fewer than 3 fields:

```java
User getById(UUID userId);
User findByEmail(String email);
```

---

## Practical: Step 3 - Implement `DefaultUserService`

```java
@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {
    private final UserRepository userRepository;

    @Override
    public User create(CreateUserCommand command) {
        userRepository.findByEmail(command.email())
            .ifPresent(existing -> {
                throw new UserAlreadyExistsException(command.email());
            });

        User user = User.builder()
            .name(command.name())
            .email(command.email())
            .build();

        return userRepository.save(user);
    }

    @Override
    public User patch(PatchUserCommand command) {
        User current = userRepository.findById(command.id())
            .orElseThrow(() -> new UserNotFoundException(command.id()));

        User updated = current.toBuilder()
            .name(command.name() != null ? command.name() : current.name())
            .email(command.email() != null ? command.email() : current.email())
            .build();

        return userRepository.save(updated);
    }

    @Override
    public User getById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public Set<User> find(PageQuery query) {
        return userRepository.find(query);
    }
}
```

Guidance:
- Annotation usage is fine; avoid framework types in contracts and core business logic.
- Assume repository adapter already translates infra exceptions to domain exceptions.

---

## Practical: Domain-only exception boundary

Service-facing contract:

```java
public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    User save(User user);
    Set<User> find(PageQuery query);
}
```

Rule:
- If persistence throws framework exceptions, translate them in the repository adapter to domain exceptions (for example `DuplicateEmailException`, `UserPersistenceException`) before they reach `DefaultUserService`.

---

## Service Test Guidance

- Instantiate the concrete service (`Default...Service`) with mocked collaborators for unit tests.
- Keep tests service-focused; persistence behavior belongs in repository integration tests.
- Follow naming, assertion, and parameterized-test conventions from `.github/skills/write-test-cases/SKILL.md`.

---

## Checklist

- interface is written before implementation (`UserService` then `DefaultUserService`)
- service contracts use command/query objects, or fewer-than-3 primitive fields
- return types are domain models only
- no framework classes are used in service contracts/business logic (annotations are acceptable)
- business rules are implemented in domain service/domain model
- only domain exceptions are propagated by domain services
- downstream adapter/repository translates infrastructure exceptions into domain exceptions
- write operations that span multiple persistence calls use `@Transactional`
- test cases follow `.github/skills/write-test-cases/SKILL.md`
