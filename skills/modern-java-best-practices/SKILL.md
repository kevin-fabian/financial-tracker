---
name: modern-java-best-practices
description: Apply modern Java (17+) idioms and best practices — records, sealed types, pattern matching, text blocks, switch expressions, and clean concurrency.
---

# Modern Java Best Practices

## Purpose

Write modern, readable, maintainable Java. Prefer simple, descriptive code and standard-library features over custom utilities when they make the solution clearer.

**Baseline:** Java 25. Use preview APIs only when the project explicitly enables preview features. Structured Concurrency is a preview API in Java 25.

## Core rules

1. **Use descriptive names.** Variable, parameter, and method names should explain their intent without requiring extra comments.
2. **Prefer modern Java syntax.** Use pattern matching, switch expressions, records, streams, and other modern features when they improve clarity.
3. **Prefer standard APIs.** Use the Java Time API (`java.time`), modern File API (`java.nio.file`), `Optional`, and Streams where appropriate.
4. **Prefer method references** when they are as clear as an equivalent lambda. Do not force them when a lambda is easier to understand.
5. **Use records by default** for immutable data carriers. Use classes when identity, mutable state, inheritance, framework constraints, or lifecycle behavior calls for them. Do not use records for JPA entities.
6. **Use simple class names in source code.** Add imports for types from other packages; do not write fully qualified class names inline unless needed to resolve a name conflict.
7. **Apply DRY.** Keep each piece of business logic in one place and extract reusable logic when it genuinely has multiple callers. Avoid unnecessary abstractions.
8. **Apply KISS.** Prefer the simplest implementation that meets the requirements. Avoid clever, deeply chained, or over-generalized code.
9. **Return early for invalid or terminal conditions.** Use guard clauses to reduce nesting.
10. **Do not use streams by default.** Use a loop when it is simpler, easier to debug, or better suited to stateful logic.
11. **Handle absence and errors explicitly.** Do not call `Optional.get()` without checking; do not silently swallow exceptions.
12. **Keep imports explicit and organized.** Avoid wildcard imports unless project conventions require them.

## Modern Java syntax and API examples

Examples target Java 25 unless noted. Snippets are intentionally small and use descriptive names.

### 1. `instanceof` pattern matching

Use pattern matching to test a type and bind the value in one step.

```java
if (requestBody instanceof CreateUserRequest createUserRequest) {
    userService.createUser(createUserRequest);
    return;
}

throw new IllegalArgumentException("Unsupported request body");
```

### 2. Modern `switch` expressions

Use arrow labels and switch expressions when selecting a value.

```java
String statusMessage = switch (order.status()) {
    case NEW -> "Order is waiting for processing";
    case PROCESSING -> "Order is being processed";
    case COMPLETED -> "Order is complete";
    case CANCELLED -> "Order was cancelled";
};
```

Pattern matching in `switch`:

```java
String describeValue(Object value) {
    return switch (value) {
        case String text -> "Text with " + text.length() + " characters";
        case Integer number -> "Integer value " + number;
        case null -> "No value was provided";
        default -> "Unsupported value type";
    };
}
```

### 3. Sealed classes and exhaustive handling

Use `sealed` when a domain type has a deliberately limited set of permitted implementations.

```java
sealed interface PaymentResult permits PaymentSucceeded, PaymentDeclined {}

record PaymentSucceeded(String transactionId) implements PaymentResult {}
record PaymentDeclined(String reason) implements PaymentResult {}

String describePayment(PaymentResult paymentResult) {
    return switch (paymentResult) {
        case PaymentSucceeded succeeded ->
                "Payment completed: " + succeeded.transactionId();
        case PaymentDeclined declined ->
                "Payment declined: " + declined.reason();
    };
}
```

### 4. Stream Gatherers (Java 24+)

Use Gatherers for stream transformations that need state across elements or produce a different number of results. Prefer ordinary stream operations for simple mapping and filtering.

```java
import java.util.stream.Gatherers;

List<List<Order>> orderBatches = orders.stream()
        .gather(Gatherers.windowFixed(100))
        .toList();
```

This creates fixed-size windows of up to 100 orders. The final window may contain fewer elements.

### 5. Unnamed variables and patterns (Java 22+)

Use `_` when a variable or pattern binding is intentionally unused.

```java
switch (paymentResult) {
    case PaymentSucceeded(String transactionId) ->
            auditService.recordSuccess(transactionId);
    case PaymentDeclined(_) ->
            auditService.recordDeclinedPayment();
}
```

An intentionally unused exception parameter:

```java
try {
    userService.refreshCache();
} catch (CacheUnavailableException _) {
    logger.warn("Cache refresh was skipped");
}
```

### 6. Stream collectors

Use standard collectors for grouping, partitioning, and aggregation.

```java
Map<Department, List<Employee>> employeesByDepartment =
        employees.stream()
                .collect(Collectors.groupingBy(Employee::department));
```

Count employees per department:

```java
Map<Department, Long> employeeCountByDepartment =
        employees.stream()
                .collect(Collectors.groupingBy(
                        Employee::department,
                        Collectors.counting()));
```

### 7. Sequenced Collections (Java 21+)

Use `SequencedCollection` when a collection has a defined encounter order and you need its first or last element.

```java
SequencedCollection<String> recentSearches = new ArrayList<>();
recentSearches.add("coffee");
recentSearches.add("groceries");
recentSearches.add("fuel");

String mostRecentSearch = recentSearches.getLast();
String oldestSearch = recentSearches.getFirst();
```

Get a reversed view:

```java
SequencedCollection<String> newestFirst = recentSearches.reversed();
```

### 8. Java Streams, method references, and Optional

Use streams for clear transformations and method references when they read naturally.

```java
Optional<User> activeUser = users.stream()
        .filter(User::isActive)
        .filter(user -> user.email().equalsIgnoreCase(emailAddress))
        .findFirst();
```

Handle absence without calling `Optional.get()`:

```java
User user = activeUser.orElseThrow(
        () -> new UserNotFoundException("No active user found"));
```

### 9. Date and Time API (`java.time`)

Use `java.time` types instead of legacy date/time classes. Inject a `Clock` when time-dependent behavior needs to be testable.

```java
boolean isSubscriptionExpired(Instant expirationTime, Clock clock) {
    return expirationTime.isBefore(clock.instant());
}
```

For date-only business concepts:

```java
LocalDate nextBillingDate = LocalDate.now(clock).plusMonths(1);
```

### 11. Modern File API (`java.nio.file`)

Use `Path` and `Files` for file operations, and handle I/O errors explicitly.

```java
Path reportPath = Path.of("reports", "monthly-report.txt");

if (Files.notExists(reportPath)) {
    return;
}

String reportContent = Files.readString(reportPath, StandardCharsets.UTF_8);
```

For writing:

```java
Files.writeString(
        reportPath,
        reportContent,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
```

### 12. Records as the default data carrier

Prefer records for simple immutable data structures.

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }
}
```

Use a regular class for JPA entities and types that require mutable state or identity.

### 13. Guard clauses and early returns

Validate inputs at the start of a method and return immediately for invalid or terminal cases.

```java
Optional<User> findActiveUser(String userId) {
    if (userId == null || userId.isBlank()) {
        return Optional.empty();
    }

    return userRepository.findById(userId)
            .filter(User::isActive);
}
```

For a `void` method:

```java
void sendWelcomeEmail(User user) {
    if (user == null || user.email() == null || user.email().isBlank()) {
        return;
    }

    emailService.sendWelcomeEmail(user.email());
}
```

## Decision checklist

Before finalizing Java code, verify:

- Are names descriptive enough to understand without extra explanation?
- Is there a modern Java feature that makes the code simpler and clearer?
- Can a method reference replace a lambda without reducing readability?
- Is a record suitable for this data carrier, or does the type need class semantics?
- Is business logic duplicated? Can a small, meaningful method be reused?
- Is the implementation the simplest one that meets the requirement?
- Are invalid conditions handled with early returns or clear exceptions?
- Are imports used instead of fully qualified class names in source?
- Are preview APIs explicitly enabled and justified?
