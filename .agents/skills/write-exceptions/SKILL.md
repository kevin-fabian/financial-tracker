---
name: write-exceptions
description: Workflow and best practices for writing domain-driven exception classes that name the broken business rule, stay REST-agnostic in the domain, and carry the data the web layer needs to build error responses.
---

# Exception Class Best-Practice Skill

Primary goal: write exception classes that describe **what business rule was broken**, stay free of HTTP/REST terminology in the domain package, and expose the raw identifiers the web layer needs to build a structured error response — while using the project's standard lemon-web exception hierarchy.

References:
- `.qwen/skills/write-rest-api/SKILL.md`
- `.qwen/skills/write-domain-services/SKILL.md`
- `.qwen/skills/write-test-cases/SKILL.md`

---

## The Exception Hierarchy

All exceptions in this project extend a base from `com.github.fabiankevin.lemon.web.exceptions`. The domain layer never references HTTP status codes directly — status and response shape are handled by the global exception handler.

| Base Class |Package | When to use |
|---|---|---|
| `NotFoundException` | `lemon.web.exceptions` | A lookup by ID or key returned no result. |
| `ApiException` | `lemon.web.exceptions` | A user-facing error where you need to control the HTTP status but don't need a structured `code`/`title`. |
| `BusinessRuleException` | `lemon.web.exceptions` | A consumed-facing business rule violation — carries `title` and `code` so API clients can reason about the failure programmatically. |

**Rule of thumb for choosing:**
- Resource not found → extend `NotFoundException`
- State conflict the client needs to handle → extend `BusinessRuleException`
- Generic client error with a clear status → extend `ApiException`

---

## Naming Conventions

### Name by the broken state (noun + past participle)

The exception name must tell a developer exactly what business rule failed — without the class body.

*Good:*
- `OrderAlreadyShippedException`
- `ProductOutOfStockException`
- `DuplicateEmailException`
- `PaymentExpiredException`

*Avoid:*
- `ValidateOrderFailedException` (action-oriented)
- `OrderSqlException` (technology-oriented)
- `OrderConflictException` (HTTP status leak)
- `InvalidInputBadRequestException` (HTTP status leak)
- `Customer404Exception` (HTTP status leak)

### Checklist for a good exception name

1. Ends with `Exception`.
2. Contains no HTTP status codes, no words like `BadRequest`, `Conflict`, `Gateway`.
3. Reading the name alone tells you exactly what business rule was violated.
4. Carries the raw IDs/states needed to build a helpful REST error response.

---

## Workflow

1. Determine the category: not found (`NotFoundException`), user-facing conflict (`ApiException`), business rule with code/title (`BusinessRuleException`), or pure domain (`DomainException`).
2. Pick a name that describes the broken business state.
3. Create the class under `app/src/main/java/com/fabiankevin/app/exceptions/` in a sub-package matching the domain context (e.g. `exceptions/users/`, `exceptions/shared_space/`).
4. Accept the minimum dynamic data needed for the message and for the response body.
5. Add a human-readable `super(...)` message for logs.
6. Expose the dynamic fields via getters so the global handler or controller advice can include them in the response.

---

## Constructors and Field Exposure

### Pattern A — NotFoundException (resource lookup)

When a resource is missing, accept the lookup key. `NotFoundException` already hardcodes HTTP 404.

```java
package com.fabiankevin.app.exceptions.users;

import com.github.fabiankevin.lemon.web.exceptions.NotFoundException;
import java.util.UUID;

public final class UserNotFoundException extends NotFoundException {
    private final UUID userId;

    public UserNotFoundException(UUID userId) {
        super("User with ID %s does not exist.".formatted(userId));
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
```

When the lookup is by a non-ID key (email, slug), use that key in both the message and the getter:

```java
public final class UserNotFoundException extends NotFoundException {
    private final String email;

    public UserNotFoundException(String email) {
        super("User not found for email: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
```

### Pattern B — BusinessRuleException (state conflict with code & title)

Use `BusinessRuleException` when the client needs a machine-readable `code` and a stable `title` to react to the failure. The `title` is a human-readable category; the `code` is a stable identifier the client can switch on (e.g. `ACCOUNT_INSUFFICIENT_FUNDS`).

```java
package com.fabiankevin.app.exceptions.accounts;

import com.github.fabiankevin.lemon.web.exceptions.BusinessRuleException;
import java.util.UUID;

public final class InsufficientFundsException extends BusinessRuleException {
    private final UUID accountId;
    private final String currency;

    public InsufficientFundsException(UUID accountId, String currency) {
        super(
            "Account %s has insufficient funds for charge in %s.".formatted(accountId, currency),
            422,
            "Insufficient Funds",
            "ACCOUNT_INSUFFICIENT_FUNDS"
        );
        this.accountId = accountId;
        this.currency = currency;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getCurrency() {
        return currency;
    }
}
```

### Pattern C — ApiException (user-facing, explicit status, no code needed)

Use `ApiException` when you need a specific HTTP status but don't need a machine-readable `code`. Good for simple permission or validation errors.

```java
package com.fabiankevin.app.exceptions.shared_space;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public final class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(message, 403);
    }
}
```

### Pattern D — DomainException (pure domain, no HTTP)

Use `DomainException` for invariants the client should never see as typed errors — these remain internal domain signals.

```java
package com.fabiankevin.app.exceptions;

import com.github.fabiankevin.lemon.web.exceptions.DomainException;

public final class InvalidAmountException extends DomainException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
```

---

## Static Message Exceptions (no dynamic data)

When the error has no dynamic payload and a fixed message suffices, a no-arg constructor is acceptable:

```java
package com.fabiankevin.app.exceptions.shared_space;

import com.github.fabiankevin.lemon.web.exceptions.ApiException;

public final class NotSpaceOwnerException extends ApiException {
    public NotSpaceOwnerException() {
        super("Only the space owner can perform this action", 403);
    }
}
```

Use this only when the response carries no per-invocation data. If the client needs the resource ID, use the parameterized form instead.

---

## Code Conventions Summary

- All exception classes are `final` — they represent a specific broken state, not a category to extend freely.
- Package mirrors domain sub-context: `exceptions/`, `exceptions/users/`, `exceptions/shared_space/`.
- Human-readable `super(...)` message for logs; getter-exposed fields for structured responses.
- The domain layer never calls `ResponseEntity`, `HttpStatus`, or MVC types.
- `BusinessRuleException.code` is `UPPER_SNAKE_CASE`, stable across deployments, and documented in the API error catalog.
- `BusinessRuleException.title` is a short human-readable category surfaced to API consumers.
- Use `NotFoundException` (lemon) rather than building your own 404-carrying class.
- `DomainException` has no `code`/`title` — it is not user-facing.

---

## Checklist

- Exception name is `final` and describes the broken state (noun + past participle)
- No HTTP status codes, `BadRequest`, `Conflict`, etc. in the class name
- Chooses the right base class: `NotFoundException` / `ApiException` / `BusinessRuleException` / `DomainException`
- Package mirrors the domain sub-context
- Message in `super(...)` is human-readable for logs
- Dynamic fields are exposed via getters for the response body
- `BusinessRuleException` only when a machine-readable `code` + `title` is needed
- `DomainException` used only for non-user-facing, pure domain invariants
- No MVC / HTTP types referenced from within the domain exception
