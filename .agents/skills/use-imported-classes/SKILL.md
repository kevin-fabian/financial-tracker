---
name: use-imported-classes
description: Enforce using imported class names (e.g., LocalDate.now()) instead of fully qualified names (e.g., java.time.LocalDate.now()) in all Java classes.
---

# Use Imported Classes Skill

Always use the simple class name (imported form) instead of the fully qualified class name when calling static methods or accessing members in Java code.

## Core Rule

When a class is imported at the top of a Java file, reference it by its simple name — never its fully qualified name — in method calls, field access, casts, or type references.

```java
// Bad — fully qualified name used despite import
import java.time.LocalDate;

LocalDate now = java.time.LocalDate.now();
```

```java
// Good — simple name used after import
import java.time.LocalDate;

LocalDate now = LocalDate.now();
```

---

## Mandatory Guidance

1. **Static method calls:** use the simple class name, not the FQN.
2. **Field/member access:** use the simple class name on the reference expression.
3. **Casts and type references:** use the simple class name.
4. **Lambda parameter types:** use the simple class name.
5. **Method references:** use the simple class name (e.g., `LocalDate::parse`).
6. **Never use FQNs for imported types** — even if it "works", it violates readability conventions.

---

## Examples

### Avoid fully qualified names for imported types

```java
// Bad
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateUtil {
    public static long daysBetween(LocalDate start, LocalDate end) {
        return java.time.LocalDate.now().until(end, ChronoUnit.DAYS);
    }
}
```

```java
// Good
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateUtil {
    public static long daysBetween(LocalDate start, LocalDate end) {
        return LocalDate.now().until(end, ChronoUnit.DAYS);
    }
}
```

### Avoid FQNs in method references

```java
// Bad
import java.util.function.Function;

Function<String, java.time.LocalDateTime> parser = java.time.LocalDateTime::parse;
```

```java
// Good
import java.util.function.Function;

Function<String, java.time.LocalDateTime> parser = LocalDateTime::parse;
```

### Avoid FQNs in casts

```java
// Bad
import java.util.List;

Object obj = getSomething();
List<String> list = (java.util.List<String>) obj;
```

```java
// Good
import java.util.List;

Object obj = getSomething();
List<String> list = (List<String>) obj;
```

### Avoid FQNs in lambda expressions

```java
// Bad
import java.util.stream.Stream;

Stream<String> stream = Stream.of("a", "b");
stream.filter(java.util.Objects::nonNull);
```

```java
// Good
import java.util.stream.Stream;

Stream<String> stream = Stream.of("a", "b");
stream.filter(Objects::nonNull);
```

---

## Practical Constraints

- This rule applies to **all Java source files** in the project — controllers, services, repositories, entities, DTOs, tests, and config classes.
- If a class is imported (via `import` statement or wildcard), always use its simple name.
- The only acceptable use of fully qualified names is when resolving **name collisions** between two imported classes with the same simple name but different packages. In that rare case, use the FQN only for the conflicting reference.
- When adding a new import, ensure all existing references to that class in the file are updated to use the simple name.

---

## Why

- **Readability:** Simple names are shorter and cleaner, making code easier to scan.
- **Consistency:** The codebase should look uniform — mixing FQNs with simple names is visually noisy.
- **Maintainability:** If a package name changes (e.g., restructuring), only the `import` statement needs updating, not every usage site.
