---
name: use-java-optional
description: Enforce using Java Optional instead of manual null handling whenever possible.
---

# Java Optional-First Skill

Use `Optional` as the default approach for nullable values. Avoid manual `null` checks unless a framework/API contract forces them.

## Core Rule

Prefer `Optional`-based flow over:

- `if (x == null) ...`
- `x != null ? ... : ...`
- nested null guards

If a value can be absent, model that with `Optional<T>` and compose behavior with `map`, `flatMap`, `filter`, `or`, `orElseGet`, `orElseThrow`, `ifPresent`, and `ifPresentOrElse`.

---

## Mandatory Guidance

1. **Method return values:** return `Optional<T>` for absent/present results instead of `null`.
2. **Service/repository lookups:** use Optional chaining, not imperative null branching.
3. **Fallbacks:** prefer `orElseGet(...)` over eager `orElse(...)` when fallback creation is non-trivial.
4. **Errors:** use `orElseThrow(...)` for required values instead of custom null-check + throw blocks.
5. **Transformation pipelines:** use `map/flatMap/filter` to keep logic declarative and safe.
6. **Never call `get()`** without proving presence in the same expression flow.

---

## Examples

### Avoid manual null handling

```java
// Bad
User user = repository.findById(id);
if (user == null) {
    throw new NotFoundException("User not found");
}
return user.getEmail();
```

```java
// Good
return repository.findById(id)
        .map(User::getEmail)
        .orElseThrow(() -> new NotFoundException("User not found"));
```

### Avoid nested null checks

```java
// Bad
if (order != null && order.getCustomer() != null && order.getCustomer().getEmail() != null) {
    return order.getCustomer().getEmail();
}
return "unknown";
```

```java
// Good
return Optional.ofNullable(order)
        .map(Order::getCustomer)
        .map(Customer::getEmail)
        .orElse("unknown");
```

### Prefer lazy fallback

```java
return repository.findById(id)
        .orElseGet(() -> createDefaultUser(id));
```

### Use `ifPresentOrElse` for side-effect branches

Use `ifPresentOrElse` when both present and absent paths are side effects (logging, metrics, notifications, commands), instead of manual null branching.

```java
// Bad
User user = repository.findById(id);
if (user != null) {
    auditService.recordLogin(user.getId());
} else {
    auditService.recordMissingUser(id);
}
```

```java
// Good
repository.findById(id)
        .ifPresentOrElse(
                user -> auditService.recordLogin(user.getId()),
                () -> auditService.recordMissingUser(id));
```

---

## Practical Constraints

- Keep entity fields and DTO fields as plain types unless project conventions explicitly use `Optional` fields.
- For APIs/frameworks that require `null`, convert at boundaries only; keep internal logic Optional-first.
- For collections, prefer empty collections over `null`; use `Optional` for absent singular values.
