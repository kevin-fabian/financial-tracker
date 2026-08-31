---
name: write-domain-models
description: Workflow and best practices for writing domain model records with builder, constructor invariants, and static factories.
---

# Domain Model Record Best-Practice Skill

Primary goal: write domain model records that enforce invariants early, expose a fluent builder API, and stay framework-free.

## Workflow

1. Define the model as a Java `record` in the `models` package (e.g. `Category`, `Transaction`, `CategorySummary`).
2. Annotate with `@Builder(toBuilder = true)` for fluent construction and immutable updates.
3. Add constructor invariants that reject null or invalid state using `Optional.ofNullable(...).filter(...).orElseThrow(...)`.
4. Provide a static factory method (`of(...)`) for the most common construction path with sensible defaults for audit fields.
5. For summary / projection variants that extend a base model, include all original fields plus the computed ones (e.g. `amount`, `percentage`, `totalTransactions`).
6. Add semantic utility methods when they express domain meaning directly — e.g. `boolean isExpense()` on `CategorySummary` so callers don't couple to enum comparison logic. Keep them pure and side-effect free; delegate to fields already present in the record.

---

## Mandatory Rules

- Always use Java records for domain models, never classes.
- Always annotate with `@Builder(toBuilder = true)`.
- Always enforce non-null / non-blank invariants in the compact constructor.
- Always provide a static factory (`of(...)`) for the primary write path.
- Add semantic utility methods that express domain meaning directly — place them on the record so callers don't replicate enum/field-comparison logic outside the model (e.g. `CategorySummary#isExpense()`, `Transaction#isCredit()`).
- Return domain models from services and repositories — never leak entities or DTOs into the domain layer.
- Use `UUID` for identity, `Instant` for timestamps, `LocalDate` for date-only values, and `EnumType.STRING` for enums.
- Keep domain models free of Spring / JPA / HTTP annotations and types.

---

## Practical: Basic domain model

```java
import java.util.Objects;

@Builder(toBuilder = true)
public record Category(
        UUID id,
        String name,
        TransactionType type,
        UUID userId,
        String icon,
        boolean active,
        boolean system,
        Instant createdAt,
        Instant updatedAt
) {
    public Category {
        Objects.requireNonNull(name, "Category name is required");
        Objects.requireNonNull(type, "Category type is required");
        Objects.requireNonNull(type, "user is required");
    }

    public static Category of(String name, TransactionType type, UUID userId, String icon) {
        return Category.builder()
                .name(name)
                .type(type)
                .userId(userId)
                .icon(icon)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
```

---

## Practical: Summary / projection variant

When a model needs computed fields (aggregations, percentages, counts), extend the base record with the same structure:

```java
@Builder(toBuilder = true)
public record CategorySummary(
        UUID id,
        String name,
        TransactionType type,
        UUID userId,
        String icon,
        boolean active,
        boolean system,
        double amount,
        double percentage,
        int totalTransactions
) {
    public CategorySummary {
        Objects.requireNonNull(name, "Category name is required");
        Objects.requireNonNull(type, "Category type is required");
        Objects.requireNonNull(type, "user is required");
    }

    public boolean isExpense() {
        return type == TransactionType.EXPENSE;
    }

    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }
}
```

Notes:
- Summary records reuse the same invariant checks as the base model.
- Computed fields (`amount`, `percentage`, `totalTransactions`) do not need constructor validation — they are populated by JPQL constructor expressions or service logic.
- Use `double` for monetary aggregates in domain models; reserve `BigDecimal` for entity columns and DTOs that require exact precision.
- Semantic utility methods like `isExpense()` / `isIncome()` encapsulate type-comparison logic on the model so services and callers don't scatter enum checks across the codebase.

---

## Practical: Using the builder

```java
// Full construction
Category category = Category.builder()
        .id(UUID.randomUUID())
        .name("Food")
        .type(TransactionType.EXPENSE)
        .userId(userId)
        .icon("🍔")
        .active(true)
        .system(false)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

// Immutable update via toBuilder
Category updated = category.toBuilder()
        .icon("🍕")
        .updatedAt(Instant.now())
        .build();

// Summary projection from JPQL
// SELECT new com.fabiankevin.app.models.CategorySummary(
//     c.id, c.name, c.type, c.user, c.icon, c.active, c.system,
//     COALESCE(SUM(t.amount), 0.0), COUNT(t.id),
//     (COALESCE(SUM(t.amount), 0.0) / :totalUserSpend * 100.0)
// ) FROM Category c LEFT JOIN Transaction t ...
```

---

## Checklist

- domain model is a Java `record`
- annotated with `@Builder(toBuilder = true)`
- compact constructor enforces non-null / non-blank invariants
- static factory (`of(...)`) provided for the primary write path
- semantic utility methods (e.g. `isExpense`, `isCredit`) encapsulate domain meaning and type checks on the model
- summary / projection variants include all base fields plus computed ones
- no Spring / JPA / HTTP types leak into the domain model
- entity↔domain conversion methods (`from(...)`, `toModel()`) live on the entity, not the domain model
