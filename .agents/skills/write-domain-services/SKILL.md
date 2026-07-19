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

### Test Structure

- Use `@ExtendWith(MockitoExtension.class)` for plain unit tests.
- Inject mocks with `@Mock` for each collaborator (repositories, clients, other services).
- Instantiate the service under test with `@InjectMocks` (`Default...Service`).
- Keep tests service-focused; persistence behavior belongs in repository integration tests.
- Follow naming conventions from `.github/skills/write-test-cases/SKILL.md`.

### Grouping and Ordering with `@Nested`

Group tests using `@Nested` inner classes when a method has more than 2 test cases. Each `@Nested` class is named after the method under test (e.g., `class CreateUser { }`).

Within each `@Nested` group, order test methods as:
1. **Happy path** — the primary success scenario; assert all returned values to verify mapping and delegation.
2. **Positive scenarios** — alternative valid inputs (null handling, defaults, boundary values).
3. **Negative scenarios** — exceptions thrown, edge cases, unauthorized access.

### Test Case Count per Method Type

| Method type | Minimum tests | Guidance |
|---|---|---|
| Pure delegation (no logic, returns repository result as-is) | 2 | One happy path (non-empty result) + one empty/default result. |
| Common repository methods (`save`, `retrieve*`, `delete*`) | 2–3 | Happy path + not-found case + edge case (null input, empty collection, etc.). |
| Methods with domain logic | 5+ | One happy path + all exception branches + edge cases. Cover every `throw` in the method. |

### Asserting the Happy Path

For happy-path tests, **always assert every field** of the returned domain model to verify:
- Correct mapping from entity/model to response DTO (when mapping occurs).
- Correct delegation to collaborators (use `verify(...)`).
- All derived/computed values (calculated fields, defaults applied).

Example:

```java
@Test
void givenValidCommand_thenReturnsMappedResult() {
    // arrange
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.of(entity));
    when(client.getUsersByIds(any())).thenReturn(List.of(user));

    // act
    Summary result = service.getById(id);

    // assert — verify every field
    assertNotNull(result);
    assertEquals(id, result.id());
    assertEquals("expected Name", result.name());
    assertEquals(SharingMode.EVEN_SHARE, result.sharingMode());
    assertEquals(3, result.items().size());
    assertTrue(result.active());

    // verify delegation
    verify(repository).findById(id);
    verify(client).getUsersByIds(any());
}
```

### Testing Methods with Logic

For methods containing business rules, write one test per exception plus edge cases:

```java
@Nested
class KickPartyMember {

    @Test
    void givenPartyLeaderKicksMember_thenMemberIsRemoved() {
        // happy path: leader kicks non-leader member
        // assert all fields of the saved entity via ArgumentCaptor
    }

    @Test
    void givenMemberKicksThemselves_thenMemberIsRemoved() {
        // positive scenario: self-removal is allowed
    }

    @Test
    void givenLeaderKicksThemselves_thenThrows() {
        // negative: CannotRemoveOwnerException
        assertThrows(CannotRemoveOwnerException.class,
            () -> service.kickPartyMember(partyId, leaderId, leaderId));
        verify(repository, never()).save(any());
    }

    @Test
    void givenNonLeaderAttemptsKick_thenThrows() {
        // negative: ForbiddenException
        assertThrows(ForbiddenException.class,
            () -> service.kickPartyMember(partyId, targetId, nonLeaderId));
        verify(repository, never()).save(any());
    }
}
```

### Verifying Delegation and Side Effects

- Use `verify(collaborator).method(args)` to confirm a collaborator was called.
- Use `verify(collaborator, never()).method(args)` in negative tests to confirm no side effects occurred.
- Use `ArgumentCaptor<T>` to inspect the exact argument passed to a collaborator (e.g., the entity saved by `repository.save(...)`).

### Parameterized Tests

When multiple inputs produce identical behavior, use `@ParameterizedTest`:

```java
@ParameterizedTest
@NullAndEmptySource
@ValueSource(strings = {" ", "  "})
void givenBlankName_thenThrows(String blankName) {
    assertThrows(InvalidNameException.class,
        () -> service.create(new CreateUserCommand(blankName, "email@test.com")));
}
```

### Complete Example

```java
@ExtendWith(MockitoExtension.class)
class DefaultPartyServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DefaultPartyService service;

    @Nested
    class OrganizeParty {

        @Test
        void givenValidCommand_thenCreatesPartyWithOwnerAsMember() {
            // happy path — assert all fields
        }

        @Test
        void givenNullPartyName_thenUsesDefaultName() {
            // positive: default value applied
        }

        @Test
        void givenNullPartyLeaderId_thenThrows() {
            // negative: constructor invariant
            assertThrows(NullPointerException.class,
                () -> new OrganizePartyCommand(null, "Name", SharingMode.EVEN_SHARE));
        }

        @Test
        void givenLeaderAlreadyInParty_thenReturnsExistingParty() {
            // positive: idempotent behavior
        }
    }

    @Nested
    class GetPartyMembersUserId {

        @Test
        void givenUserId_thenDelegatesAndReturnsResult() {
            // delegation test — happy path
        }

        @Test
        void givenRepositoryReturnsEmpty_thenReturnsEmptyList() {
            // delegation test — empty result
        }
    }
}
```

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
