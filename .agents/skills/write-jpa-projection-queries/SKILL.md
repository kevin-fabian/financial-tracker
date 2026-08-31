---
name: write-jpa-projection-queries
description: Workflow and best practices for writing JPA projection queries using lightweight DTOs with primitive types, String, UUID, and COALESCE for default numeric values.
---

# JPA Projection Query Best-Practice Skill

Primary goal: write JPA projection queries that return lightweight, read-optimized DTOs using only primitive types (`int`, `double`), `String`, `UUID`, and `boolean` — avoiding enums, entity references, or complex domain objects in the projection layer.

References:
- `.github/skills/write-jpa-entities/SKILL.md` (entity mapping conventions)
- `.github/skills/write-jpa-domain-repositories/SKILL.md` (domain repository patterns)
- `.github/skills/write-test-cases/SKILL.md` (test naming, assertion, and parameterized-test conventions)

## Projection Architecture

```
JPA Repository (@Query + @Param)
    |
    v
Projection DTO (record with primitives, String, UUID, boolean)
    |  — fields match JPQL SELECT column aliases exactly
    |  — use COALESCE for default numeric values
    |  — no enums, no entity references, no domain objects
    v
Domain layer converts projection → domain model (if needed)
```

---

## Mandatory Rules

- Projection DTOs must use only primitive types (`int`, `double`), `String`, `UUID`, and `boolean`.
- Never use enums, entity types, or domain models directly in projection DTOs — convert at the domain layer instead.
- JPQL `SELECT` column aliases must match the projection DTO field names exactly (case-sensitive).
- Use `COALESCE` for all aggregate expressions (`SUM`, `COUNT`, `AVG`) to provide default values for null results.
- Cast `COUNT` results to `int` using `CAST(COALESCE(COUNT(...), 0) AS int)` to match Java `int` fields.
- Cast `SUM` results to `double` using `COALESCE(SUM(...), 0.0)` to match Java `double` fields.
- Projection DTOs should be `record` types with `@Builder(toBuilder = true)` from Lombok.
- Place projection DTOs under `persistence/entities/projections/` package.
- Return projections from `JpaRepository` methods — never leak them through service interfaces.
- Use `@Param` annotations for all query parameters in custom `@Query` methods.

---

## Practical: Step 1 - Projection DTO

Projection DTOs are lightweight, read-only records using only primitive types, `String`, `UUID`, and `boolean`.

```java
package com.fabiankevin.app.persistence.entities.projections;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CategorySummaryProjection(
        UUID id,
        String name,
        String type,          // enum stored as String, not TransactionType
        UUID userId,
        String icon,
        boolean active,
        boolean system,
        double amount,
        int totalTransactions
) {
}
```

Guidance:
- Use `String` instead of enums — the JPQL query projects the enum as a string via `STR(enumField)` or the enum name directly.
- Use `boolean` (not `Boolean`) for true/false fields — JPA projections map directly.
- Use `double` (not `Double`) for monetary/numeric aggregates — paired with `COALESCE(..., 0.0)`.
- Use `int` (not `Integer`) for counts — paired with `CAST(COALESCE(..., 0) AS int)`.
- Keep field names matching the JPQL `AS` aliases exactly.

---

## Practical: Step 2 - Projection DTO with Aggregates

When projecting aggregated data (totals, counts, averages), use `COALESCE` to guarantee non-null defaults.

```java
package com.fabiankevin.app.persistence.entities.projections;

import lombok.Builder;

@Builder(toBuilder = true)
public record SummaryPointProjection(
        String label,
        double total
) {
    // Constructor overload for int → String label conversion
    public SummaryPointProjection(int label, double total) {
        this(String.valueOf(label), total);
    }

    public SummaryPoint toModel() {
        return new SummaryPoint(label, total);
    }
}
```

Guidance:
- `COALESCE(SUM(...), 0.0)` ensures `double total` is never null.
- `label` is `String` — the JPQL query can use `MONTH(...)`, `YEAR(...)`, `DAY(...)`, or any expression, all projected as `String`.
- Provide a secondary constructor if the JPQL expression returns a different type (e.g., `int` month number → `String` label).
- Include `toModel()` only when the projection needs conversion to a domain model.

---

## Practical: Step 3 - Projection DTO with Paginated Entity Fields

When projecting fields directly from an entity (not aggregated), include all fields the query selects.

```java
package com.fabiankevin.app.persistence.entities.projections;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record AccountSummaryProjection(
        UUID id,
        String name,
        UUID userId,
        String currency,
        String type,
        boolean active,
        boolean system,
        double totalAmount,
        int totalTransactions
) {
}
```

Guidance:
- Entity scalar fields (`name`, `currency`) map to `String`.
- Entity UUID fields (`id`, `user`) map to `UUID`.
- Entity boolean fields (`active`, `system`) map to `boolean`.
- Entity enum fields (`type`) map to `String` — use `STR(entity.enumField)` in JPQL if needed.
- Aggregated fields (`totalAmount`, `totalTransactions`) use `COALESCE` with matching primitive types.

---

## Practical: Step 4 - JPA Repository Query

Write the `@Query` method in the `JpaRepository` interface. Column aliases must match projection DTO field names.

**Paginated projection with LEFT JOIN and date filter:**

```java
@Query("""
        SELECT c.id, c.name, STR(c.transactionType), c.user, c.icon, c.active, c.system,
            COALESCE(SUM(t.amount), 0.0),
            CAST(COALESCE(COUNT(t.id), 0) AS int)
        FROM CategoryEntity c
        LEFT JOIN TransactionEntity t ON t.category.id = c.id
            AND t.transactionDate >= :monthStart
            AND t.transactionDate <= :monthEnd
        WHERE c.user = :user
        AND (:type IS NULL OR c.transactionType = :type)
        GROUP BY c
        """)
Page<CategorySummaryProjection> findAllByUserIdAndTransactionTypeWithSummary(
        @Param("user") UUID userId,
        @Param("type") TransactionType type,
        @Param("monthStart") LocalDate monthStart,
        @Param("monthEnd") LocalDate monthEnd,
        Pageable pageable);
```

Guidance:
- `GROUP BY c` groups by the entire entity — Spring Data JPA resolves the column order automatically.
- `LEFT JOIN` with conditions on the joined table (`t.transactionDate >= ...`) filters the aggregation, not the base entity. This keeps categories with zero transactions in the result.
- `STR(c.transactionType)` converts the enum to `String` for the projection.
- `:type IS NULL OR c.transactionType = :type` is the standard optional filter pattern — `null` means "no filter".
- Column order in `SELECT` must match the projection DTO constructor parameter order.

**Streamable projection with date range aggregation:**

```java
@Query("""
        SELECT STR(t.category.transactionType) AS label, COALESCE(SUM(t.amount), 0.0) AS total
        FROM TransactionEntity t
        WHERE t.account.user = :user
          AND t.transactionDate BETWEEN :from AND :to
          AND (:accountId IS NULL OR t.account.id = :accountId)
          AND (:categoryId IS NULL OR t.category.id = :categoryId)
        GROUP BY t.category.transactionType
        """)
Streamable<SummaryPointProjection> sumByTypeAndDateRange(
        @Param("user") UUID userId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("accountId") UUID accountId,
        @Param("categoryId") UUID categoryId);
```

Guidance:
- `Streamable<T>` is suitable for unbounded result sets — it streams results without loading all into memory.
- `AS label` and `AS total` aliases must match `SummaryPointProjection` field names exactly.
- `BETWEEN :from AND :to` is inclusive on both ends.
- Multiple optional filters use the `(:param IS NULL OR entity.field = :param)` pattern.

**Grouped by date function (MONTH, YEAR, DAY):**

```java
@Query("""
        SELECT MONTH(t.transactionDate) AS label, COALESCE(SUM(t.amount), 0.0) AS sum
        FROM TransactionEntity t
        WHERE t.transactionDate BETWEEN :from AND :to
          AND t.account.user IN :userIds
          AND (:type IS NULL OR t.category.transactionType = :type)
        GROUP BY MONTH(t.transactionDate)
        """)
Streamable<SummaryPointProjection> getSummaryByDateRangeAndUserIdGroupedByMonth(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("userIds") List<UUID> userIds,
        @Param("type") TransactionType type);
```

Guidance:
- `MONTH(...)`, `YEAR(...)`, `DAY(...)` return `int` — the `SummaryPointProjection` constructor handles `int → String` conversion.
- `IN :userIds` supports multi-user aggregation (e.g., team accounts).
- Return `Streamable` for date-grouped aggregates — the result set size is bounded by calendar periods (12 months, N years, 31 days).

---

## Practical: Step 5 - Converting Projections to Domain Models

Projection DTOs are persistence-layer types. Convert them to domain models in the service or repository adapter layer.

```java
// In SummaryPointProjection.java
public SummaryPoint toModel() {
    return new SummaryPoint(label, total);
}
```

Or in the domain repository adapter:

```java
@Override
public List<SummaryPoint> getSummaryByCategory(LocalDate from, LocalDate to) {
    return transactionRepository
        .getSummaryByDateRangeAndUserIdGroupedByCategory(from, to, List.of(userId), null)
        .stream()
        .map(SummaryPointProjection::toModel)
        .toList();
}
```

Guidance:
- Keep `toModel()` on the projection DTO when the conversion is straightforward (field-for-field mapping).
- Use the adapter/repository layer for complex conversions that involve lookups or domain logic.
- Never return projection DTOs through service interfaces — always convert to domain models.

---

## COALESCE Reference

Use `COALESCE` for every aggregate expression to guarantee non-null defaults matching the projection field type.

| Java Field Type | JPQL Expression | Default Value |
|-----------------|-----------------|---------------|
| `double` | `COALESCE(SUM(field), 0.0)` | `0.0` |
| `int` | `CAST(COALESCE(COUNT(field), 0) AS int)` | `0` |
| `double` | `COALESCE(AVG(field), 0.0)` | `0.0` |
| `long` | `CAST(COALESCE(COUNT(*), 0L) AS long)` | `0L` |

Notes:
- `COUNT(*)` counts rows; `COUNT(field)` counts non-null values of `field`. Use `COUNT(field)` when the field comes from a `LEFT JOIN` to exclude rows where the joined column is null.
- `CAST(... AS int)` is required because Hibernate may return `Long` for `COUNT` — the projection DTO expects `int`.

---

## Enum Projection Pattern

Projection DTOs must never use enums. Convert enums to `String` in the JPQL query.

```java
// Projection DTO — String, not TransactionType
@Builder(toBuilder = true)
public record CategorySummaryProjection(
        UUID id,
        String name,
        String type,    // String, NOT TransactionType
        UUID userId,
        double amount,
        int totalTransactions
) {}

// JPQL — use STR() to convert enum to String
@Query("""
        SELECT c.id, c.name, STR(c.transactionType), c.user,
            COALESCE(SUM(t.amount), 0.0),
            CAST(COALESCE(COUNT(t.id), 0) AS int)
        FROM CategoryEntity c
        LEFT JOIN TransactionEntity t ON t.category.id = c.id
        WHERE c.user = :user
        GROUP BY c
        """)
Page<CategorySummaryProjection> findAllByUserIdWithSummary(
        @Param("user") UUID userId,
        Pageable pageable);
```

Guidance:
- `STR(enumField)` is the JPQL function to convert an `@Enumerated(EnumType.STRING)` field to a `String`.
- The domain layer can reconstruct the enum from the string if needed (`TransactionType.valueOf(projection.type())`).

---

## Checklist

- projection DTO uses only `int`, `double`, `String`, `UUID`, `boolean` — no enums, no entities, no domain models
- projection DTO is a `record` with `@Builder(toBuilder = true)`
- JPQL `SELECT` column aliases match projection DTO field names exactly
- `COALESCE` wraps all `SUM`, `COUNT`, `AVG` expressions with matching default types (`0.0`, `0`, `0L`)
- `CAST(COALESCE(COUNT(...), 0) AS int)` for `int` count fields
- `STR(enumField)` converts enums to `String` in JPQL
- `LEFT JOIN` conditions on joined table use `ON` clause (not `WHERE`) to preserve base entities with zero aggregates
- `:param IS NULL OR entity.field = :param` for optional filter parameters
- `@Param` annotations on all query parameters
- Projection DTOs placed under `persistence/entities/projections/`
- Projections are converted to domain models before leaving the persistence layer
- `Streamable<T>` for unbounded or large result sets; `Page<T>` for paginated results; `List<T>` for bounded results
