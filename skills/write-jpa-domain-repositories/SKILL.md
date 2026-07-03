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

---

## Practical: Step 4 - Test `DefaultUserRepository` with `@DataJpaTest` + `write-test-cases` conventions

```java
@DataJpaTest
class DefaultUserRepositoryTest {
    @MockitoSpyBean
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserRepository repository;

    @TestConfiguration
    static class ContextConfiguration {
        @Bean
        UserRepository userRepository(UserJpaRepository userJpaRepository) {
            return new DefaultUserRepository(userJpaRepository);
        }
    }

    @Test
    void findById_existingId_returnsUser() {
        User user = User.builder()
            .email("john@acme.com")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        User saved = repository.save(user);

        Optional<User> result = repository.findById(saved.id());

        assertTrue(result.isPresent(), "User should be present for an existing ID");
        assertEquals("john@acme.com", result.get().email(), "Email should match the persisted value");
        verify(userJpaRepository, times(1)).findById(saved.id());
    }

    @Test
    void save_validUser_savesSuccessfully() {
        User user = User.builder()
            .email("john@acme.com")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        User saved = repository.save(user);

        assertNotNull(saved.id(), "ID should be generated after saving");
        assertNotNull(saved.createdAt(), "createdAt should be persisted");
        assertNotNull(saved.updatedAt(), "updatedAt should be persisted");
    }
}
```

Coverage checklist:
- `findById` found/not found
- `findByEmail` found/not found
- `save` maps domain → entity → domain
- exception translation path (if used)
- `deleteById` delegation
- use real JPA infrastructure via `@DataJpaTest` (no mocked adapter under test)
- do not set ID manually; assert generated IDs
- follow `.github/skills/write-test-cases/SKILL.md` for naming, assertion messages, parameterized tests, and AssertJ usage for complex/collection assertions
- strictly no comments in test cases
