---
name: write-jpa-entities
description: Workflow and best practices for writing JPA entities with correct mapping, ownership, and fetch strategies.
---

# JPA Entity Best-Practice Skill

Primary goal: implement JPA entities that are explicit, predictable, and safe in production behavior.

## Workflow

1. Define the entity as a Java class with `@Entity` and `@Table(name = "...")`.
2. Add core identity and audit fields (`id`, `createdAt`, `updatedAt`) with explicit `@Column` constraints, using `Instant` for timestamps.
3. Map scalar fields with `@Column(...)` and map enums with `@Enumerated(EnumType.STRING)`.
4. Design relationships by deciding the owning side first, then apply `@JoinColumn` only on the owning side.
5. Set fetch strategy intentionally (default to `LAZY`, use `EAGER` only when bounded and justified).
6. Add `toModel()` / `from(...)` conversion methods using builders for stable domain/entity mapping.
7. Confirm nullability, column lengths, and relationship ownership are aligned with DB constraints.

---

## Mandatory Rules

- Use Java classes, not records, for entities.
- Use `@GeneratedValue(strategy = GenerationType.UUID)` for UUID primary keys.
- Always define `@Column` details (`nullable`, `length`, precision/scale when relevant).
- Use `EnumType.STRING` for enum persistence.
- Use `Instant` for timestamp/date-time fields (e.g., `createdAt`, `updatedAt`, `deletedAt`).
- Use `LocalDate` for date-only fields with no time-zone/time component (e.g., `birthDate`).
- Always use `Set` (never `List`) for JPA collection associations such as `@OneToMany` and `@ManyToMany`.
- Model reusable value objects with `@Embeddable` + `@Embedded` instead of flattening repeated fields.
- Avoid bidirectional relationships unless they provide real query/navigation value.
- Keep owning side updates explicit to avoid hidden relationship bugs.

---

## Practical: `@OneToMany` + `@ManyToOne` (owning side is `@ManyToOne`)

This is the recommended default for parent/child modeling.

```java
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItemEntity> items = new HashSet<>();
}

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;
}
```

### Fetch guidance for `@ManyToOne`

- Default: `FetchType.LAZY`.
- Use `FetchType.EAGER` only when the cardinality is definitely small and bounded (for example, always below 10 related objects in the real domain usage), and the associated object is always needed in normal read flows.

Example bounded reference:

```java
@ManyToOne(fetch = FetchType.EAGER, optional = false)
@JoinColumn(name = "country_id", nullable = false)
private CountryEntity country;
```

---

## Practical: `@OneToMany` where `@OneToMany` side is owning side

Use this only for unidirectional collection ownership where the parent should control the FK directly.

```java
@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "project_id", nullable = false) // FK stored in task table
    private Set<TaskEntity> tasks = new HashSet<>();
}
```

Notes:
- Do not use `mappedBy` here (the `@OneToMany` is owning).
- Keep this association `LAZY` unless there is a strong bounded-use reason.
- Keep `Set` as the enforced collection type; do not switch to `List`.
- Prefer the default `@ManyToOne`-owning model when both directions are needed.

---

## Practical: `@ManyToMany` with owning side

Use `Set` on both sides and define `@JoinTable` only on the owning side.

```java
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();
}

@Entity
@Table(name = "roles")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<UserEntity> users = new HashSet<>();
}
```

Notes:
- Keep the join table ownership in one place only (the owning side).
- Keep `LAZY` by default to avoid unnecessary graph loading.

---

## Practical: `@Embeddable` value object

Use `@Embeddable` for value objects that do not have their own identity and lifecycle.

```java
@Embeddable
public class AddressEmbeddable {
    @Column(name = "street", nullable = false, length = 255)
    private String street;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;
}

@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    private AddressEmbeddable address;
}
```

If embedding the same type multiple times, use `@AttributeOverrides` to keep column names explicit.

---

## Practical: `Instant` vs `LocalDate`

Use `Instant` for audit/timestamp fields and `LocalDate` for business dates like birthday.

```java
@Entity
@Table(name = "profiles")
public class ProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

Notes:
- Do not use `LocalDateTime` for audit timestamps in distributed systems.
- Keep `Instant` as the default timestamp type unless a strict project convention requires otherwise.

---

## Mapping Utilities

```java
public static UserEntity from(User user) {
    return UserEntity.builder()
            .id(user.id())
            .email(user.email())
            .status(user.status())
            .createdAt(user.createdAt())
            .updatedAt(user.updatedAt())
            .build();
}

public User toModel() {
    return User.builder()
            .id(this.id)
            .email(this.email)
            .status(this.status)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .build();
}
```

## Checklist

- entity uses `@Entity` + explicit `@Table(name = "...")`
- id uses UUID with `@GeneratedValue(strategy = GenerationType.UUID)`
- scalar fields define explicit `@Column` constraints
- enums use `@Enumerated(EnumType.STRING)`
- timestamps use `Instant`; date-only values use `LocalDate`
- collections use `Set` (never `List`)
- relationship owning side is explicitly chosen and `@JoinColumn` is only on owning side
- default fetch is `LAZY`; `EAGER` used only for bounded, always-needed references
- value objects use `@Embeddable` + `@Embedded` when identity is not required
- entity↔domain conversion methods (`from(...)`, `toModel()`) are present and complete
