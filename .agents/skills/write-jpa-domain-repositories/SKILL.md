---
name: write-jpa-domain-repositories
description: Workflow and best practices for implementing Spring Data JpaRepository plus framework-independent domain repositories.
---

# JPA + Domain Repository Skill

Primary goal: implement persistence adapters that keep domain services framework-agnostic while using Spring Data JPA correctly.

References:
- `.github/skills/write-jpa-entities/SKILL.md` (entity mapping, relationships, and conversion rules)
- `.github/skills/write-test-cases/SKILL.md` (general test naming, assertion, and parameterized-test conventions)

## Workflow

1. Write the `JpaRepository` interface (e.g., `UserJpaRepository`) for the entity.
2. Write the domain repository interface (e.g., `UserRepository`) in the domain layer, independent from Spring/JPA.
3. Write `DefaultUserRepository` that implements `UserRepository` and depends on `UserJpaRepository`.
4. Write test cases for `DefaultUserRepository` behavior (mapping, happy paths, and error scenarios).

---

## Mandatory Rules

- The Spring Data interface extends `JpaRepository<UserEntity, UUID>` when entity ID is UUID.
- Keep repository naming consistent with project conventions (`JpaUserRepository` or `UserJpaRepository`).
- `UserRepository` must only expose domain models (`User`, `Optional<User>`, `Set<User>`, etc.), never entities.
- `DefaultUserRepository` is the only place where entity↔domain mapping happens (`UserEntity.from(...)`, `toModel()`).
- Domain services depend on `UserRepository`, never on `JpaRepository`.
- Keep Spring-specific annotations out of domain interfaces.
- Use `Set` (not `List`) for collection return types when ordering is not required.
- Test `Default...Repository` behaviors, not Spring Data `Jpa*Repository` interfaces directly.
- **Do not explicitly set an ID when persisting a new entity.** Let JPA's `@GeneratedValue` strategy (e.g., UUID generator) assign the ID — the domain model's ID should be `null` before `save()` and populated after. Never manually assign a UUID or other generated ID to a new entity.

---

## Practical: Step 1 - `UserJpaRepository`

```java
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    Optional<UserEntity> findWithRolesById(UUID id);
}
```

Guidance:
- Add `@EntityGraph` only for concrete query use cases that need eager graph loading.
- Keep repository method names business-query oriented.

---

## Practical: Step 2 - Domain `UserRepository`

```java
public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    User save(User user);
    void deleteById(UUID id);
}
```

Guidance:
- No Spring imports/annotations.
- This interface is owned by the domain layer and used by domain services.

---

## Practical: Step 3 - `DefaultUserRepository`

```java
@Repository
@RequiredArgsConstructor
public class DefaultUserRepository implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(UserEntity::toModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(UserEntity::toModel);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(UserEntity.from(user)).toModel();
    }

    @Override
    public void deleteById(UUID id) {
        userJpaRepository.deleteById(id);
    }
}
```

Guidance:
- Catch and translate infrastructure exceptions only when there is a clear domain exception contract.
- Do not leak `UserEntity` outside this adapter.
- The incoming domain model to `save` must have a **null ID** — never set one. The generated ID is returned via `toModel()` after the JPA `save` call.

---

## Practical: Step 4 - Test `DefaultUserRepository` with `@DataJpaTest`

### Test Class Structure

Use this exact skeleton for every repository test:

```java
@Import(DefaultUserRepositoryTest.ContextConfiguration.class)
@DataJpaTest
class DefaultUserRepositoryTest {

    @MockitoSpyBean
    private JpaUserRepository jpaUserRepository;

    @Autowired
    private UserRepository userRepository;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public UserRepository userRepository(JpaUserRepository jpaUserRepository) {
            return new DefaultUserRepository(jpaUserRepository);
        }
    }
}
```

Key choices:
- **`@DataJpaTest`** — boot only JPA infrastructure (no web layer, no service beans).
- **`@MockitoSpyBean`** on the JpaRepository — wraps the real Spring bean so you can `verify(...)` delegation without replacing it.
- **`@Autowired`** the domain repository under test — injected from the nested `@TestConfiguration`.
- **`@Import(ContextConfiguration.class)`** — ensures the test picks up the bean factory.
- Tests run against the real persistence engine (H2 for `local`/`test` profiles, PostgreSQL for `local-pg` profile).

### Cross-Repository Persistence

When a repository method depends on another aggregate, autowire additional JpaRepositories to set up related data via `saveAndFlush`:

```java
@Autowired
private JpaUserRepository userRepository;

@Autowired
private JpaAccountRepository jpaAccountRepository;

@Autowired
private JpaTransactionRepository jpaTransactionRepository;
```

Use `saveAndFlush(...)` (not `save(...)`) when you need the entity committed and its generated ID available in the same test before issuing a query.

### Test Data Setup

```java
private User user;

@BeforeEach
void setUp() {
    user = User.builder()
        .email("john@acme.com")
        .name("John")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
}

```

Why `setUp` exists: creates reusable domain-model fixtures so each test arranges only what differs from the default.

If a test needs to verify delegation counts from scratch, reset the spy at the end of `setUp`:

```java
@BeforeEach
void setUp() {
    ...
    Mockito.reset(jpaUserRepository);
}
```

### Grouping with `@Nested` and Ordering

Group query/find tests with `@Nested` inner classes (one per repository method or query variant).

Ordering within each group:
1. **Happy path** (entity exists, returns result) — assert every field.
2. **Positive scenarios** (null handling, default values, alternative valid inputs).
3. **Negative scenarios** (entity not found, invalid input, throws exception).

### Asserting the Happy Path — Full Field Verification

Always persist first, then retrieve, and assert every non-generated field via `usingRecursiveComparison().ignoringFields("id")` (preferred for deep equals) **plus** a field-by-field check for computed/defaulted columns. Always assert generated IDs are non-null:

```java
@Test
void givenValidUser_persistsAndRetrievesAllFields() {
    User saved = userRepository.save(user);

    var found = userRepository.findById(saved.id()).orElseThrow();

    Assertions.assertThat(found)
        .as("retrieved user should match saved user ignoring generated id")
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(saved);

    verify(jpaUserRepository, times(1)).save(any());
    verify(jpaUserRepository, times(1)).findById(saved.id());
}
```

**AssertJ is the standard** for persistence tests (preferred over bare JUnit `assertEquals`). Use `Assertions.assertThat(...)`, `Assertions.assertThatThrownBy(...)`, `Assertions.assertThatCode(...)`.

### Scenario Patterns (per method type)

#### `save` tests
| # | Scenario | Assertion focus |
|---|----------|-----------------|
| 1 | Persist and retrieve by generated ID | Every field matches; generated `id` is non-null |
| 2 | Cascade persist (embedded collections) | Nested entities have generated IDs |
| 3 | Null or blank column mapping | `assertNull` / `assertNotNull` for optional columns; or `flush()` then re-read |
| 4 | Nullable unique key (e.g. `null` FK) | Entity persists with `null` field |
| 5 | Invalid input (null domain model) | `assertThatThrownBy(...).isInstanceOf(...)` |

#### `findById` / `findByXxx` tests
| # | Scenario | Assertion focus |
|---|----------|-----------------|
| 1 | Entity exists | Optional present; fields match; verify delegation once |
| 2 | Entity not found | Optional empty (`isEmpty()`); never throws |
| 3 | Query by partial distinct key (e.g. different user, different type) | Result is filtered correctly |

#### Custom query tests (projection / summary queries)
| # | Scenario | Assertion focus |
|---|----------|-----------------|
| 1 | Single row → summary | Assert computed values (`totalBalance`, `percentage`, `totalTransactions`) using `isCloseTo(..., within(0.01))` for doubles |
| 2 | Multiple rows → paged summaries | Map results by key; assert each row's values individually |
| 3 | No matches → empty page | `content` empty, `totalElements` zero |

#### `deleteById` tests
| # | Scenario | Assertion focus |
|---|----------|-----------------|
| 1 | Delete existing entity | `findById` returns empty afterward; verify delegation once |
| 2 | Delete non-existing entity | No exception; delegation still called (or not, depending on adapter contract) |

#### Exception translation tests (if adapter translates infra → domain)
| # | Scenario | Assertion focus |
|---|----------|-----------------|
| 1 | Duplicate key / constraint violation | Assert exact domain exception type via `assertThatThrownBy(...).isInstanceOf(...)` |
| 2 | Saving null | Assert Spring's `InvalidDataAccessApiUsageException` (or translated domain equivalent) |

### Verifying Delegation

Always confirm the adapter delegates correctly:

```java
// Positive — delegation occurred exactly once
verify(jpaUserRepository, times(1)).save(any());
verify(jpaUserRepository, times(1)).findById(saved.id());

// Negative — no delegation on invalid input
verify(jpaUserRepository, never()).save(any());
```

When the adapter translates a Spring Data exception (e.g. `DuplicateKeyException`), verify that the JpaRepository was still called before translation.

### Captor for Save Arguments

If the adapter mutates the entity before saving (e.g. sets audit timestamps), capture the entity passed to `jpaRepository.save(...)` and assert on it:

```java
ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
verify(jpaUserRepository).save(captor.capture());
assertNotNull(captor.getValue().createdAt());
```

### No Comments in Test Cases

Test bodies carry no inline comments — let assertions and scenario names speak. The arrangement is: set up data, act, assert delegations.

### Full Example (matching project conventions)

```java
@Import(DefaultUserRepositoryTest.ContextConfiguration.class)
@DataJpaTest
class DefaultUserRepositoryTest {

    @MockitoSpyBean
    private JpaUserRepository jpaUserRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @TestConfiguration
    public static class ContextConfiguration {
        @Bean
        public UserRepository userRepository(JpaUserRepository jpaUserRepository) {
            return new DefaultUserRepository(jpaUserRepository);
        }
    }

    @BeforeEach
    void setUp() {
        user = User.builder()
            .email("john@acme.com")
            .name("John Doe")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    @Test
    void save_givenValidUser_persistsAndRetrieves() {
        User saved = userRepository.save(user);

        var found = userRepository.findById(saved.id()).orElseThrow();

        Assertions.assertThat(found)
            .as("retrieved equals saved ignoring id")
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(saved);

        verify(jpaUserRepository, times(1)).save(any());
        verify(jpaUserRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenExistingUser_returnsUser() {
        User saved = userRepository.save(user);

        var found = userRepository.findById(saved.id()).orElseThrow();

        assertEquals("john@acme.com", found.email());

        verify(jpaUserRepository, times(1)).findById(saved.id());
    }

    @Test
    void findById_givenNonExisting_returnsEmpty() {
        Optional<User> found = userRepository.findById(UUID.randomUUID());

        Assertions.assertThat(found).isEmpty();
    }

    @Test
    void deleteById_givenExisting_removes() {
        User saved = userRepository.save(user);

        userRepository.deleteById(saved.id());

        Assertions.assertThat(userRepository.findById(saved.id())).isEmpty();

        verify(jpaUserRepository, times(1)).deleteById(saved.id());
    }
}
```

### Coverage Checklist

- `save` — persists + retrieves all fields, generated `id` non-null
- `findById` — found (full field match) and not found (empty Optional)
- custom queries — happy path with computed values; empty/zero-result path
- `deleteById` — existing removed; delegation verified
- cascade/Embeddable collections — every nested field persisted and retrieved
- exception translation — infra exception converted to domain exception type
- delegation — `verify(jpaRepository, times(1)).method(args)` on every happy path
- use `@Nested` grouping when a method has more than 2 test cases
- follow ordering: happy path → positive → negative
- strictly no comments in test cases
- use AssertJ (`Assertions.assertThat`, `assertThatThrownBy`, `assertThatCode`)
