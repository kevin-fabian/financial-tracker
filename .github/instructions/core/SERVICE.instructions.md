---
applyTo: '**/*Service.java'
description: 'Service interface and implementation guidelines for this repository'
---

## Writing Service Implementations Best Practices

### Do

- Follow the interface + `Default*` implementation pattern (for example, `UserService` + `DefaultUserService`).
- Keep service boundaries framework-agnostic: do not expose JPA entities or `Jpa*Repository` types in service APIs.
- Accept command objects (`services/commands/*`) instead of web DTOs; convert DTOs in the `web` layer first.
- Return domain models (`models/*`) or `Optional`/collections of domain models.
- Use `@Transactional` for write operations that span multiple persistence calls or side effects.
- For validating input, prefer explicit checks and custom exceptions over complex validation frameworks for better control and clarity.
- Use regex and utility methods for validating fields like email, phone number, etc., in the service layer when needed.

### Don't
- Don't leak persistence layer details (like JPA entities or repositories) in service interfaces.
- Don't use web layer DTOs in service APIs; Use command objects instead to decouple from web concerns.

---

## Example Service Interface and Implementation

```java
public interface UserService {
    User createUser(CreateUserCommand command);
    User retrieveById(UUID id);
}
```

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultUserService implements UserService {
    private final UserRepository userRepository;

    @Override
    public User createUser(CreateUserCommand command) {
        return userRepository.save(User.builder()
                .name(command.name())
                .email(command.email())
                .build());
    }

    @Override
    public User retrieveById(UUID id) {
        return userRepository.findById(id)
                .orThrow(UserNotFoundException::new);
    }
}
```
