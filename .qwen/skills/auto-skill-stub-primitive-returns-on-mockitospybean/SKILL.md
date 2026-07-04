---
name: stub-primitive-returns-on-mockitospybean
description: How to stub primitive return types on @MockitoSpyBean Spring Data JPA repository mocks without AopInvocationException
source: auto-skill
extracted_at: '2026-07-04T05:38:55.703Z'
---

## Problem

When using `@MockitoSpyBean` on a Spring Data JPA repository interface, Mockito's default answer returns `null` for unstubbed method calls. If the method returns a primitive type (`double`, `int`, `boolean`, etc.), Java cannot unbox `null` to a primitive, causing:

```
org.springframework.aop.AopInvocationException: Null return value from advice does not match primitive return type for: public abstract double com.fabiankevin.app.persistence.jpa_repositories.JpaTransactionRepository.sumByTypeAndDateRange(...)
```

This commonly hits aggregate queries (`SUM(...)`) that return `double` or `int`.

## Solution

Use `doReturn(...).when(mock).method(args)` (BDD-style stubbing) instead of `when(mock.method(args)).thenReturn(...)` for primitive return types on spied beans.

### Correct (primitive return type)

```java
// Use doReturn/when for primitive returns on @MockitoSpyBean
doReturn(0.0).when(jpaTransactionRepository)
        .sumByTypeAndDateRange(userId, TransactionType.EXPENSE, from, to, accountId, categoryId);

double result = transactionRepository.sumByTypeAndUserId(userId, TransactionType.EXPENSE, from, to, accountId, categoryId);
Assertions.assertThat(result).isEqualTo(0.0);
```

### Incorrect (will throw AopInvocationException)

```java
// DO NOT use when/thenReturn for primitive returns on @MockitoSpyBean
when(jpaTransactionRepository.sumByTypeAndDateRange(userId, TransactionType.EXPENSE, from, to, accountId, categoryId))
        .thenReturn(0.0);  // throws AopInvocationException
```

### When `when/thenReturn` is fine

For non-primitive return types (e.g., `Optional`, `List`, `Streamable`), `when/thenReturn` works fine:

```java
when(jpaTransactionRepository.getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, List.of(otherUserId), TransactionType.EXPENSE))
        .thenReturn(Streamable.empty());
```

## Rule of thumb

- **Primitive return types** (`double`, `int`, `boolean`, `long`, etc.) → always use `doReturn(value).when(mock).method(args)`
- **Object return types** (`Optional`, `List`, `Streamable`, `Page`, etc.) → `when(mock.method(args)).thenReturn(value)` is fine

## Verification

Run the test class to confirm no `AopInvocationException`:

```bash
./mvnw -pl app -Dtest=DefaultTransactionRepositoryTest test
```
