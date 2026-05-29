---
applyTo: '**/*Entity.java'
description: 'Guidelines for writing JPA entities in this repository'
---

## Writing JPA Entities Best Practices

### Do

- Follow entity name + `Entity` suffix pattern (e.g., `UserEntity`).
- Use classes for JPA entities with Lombok to reduce boilerplate.
- Use JPA annotations explicitly for table, column, id, and relationship mapping (e.g., `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@OneToMany`, `@ManyToOne`, `@JoinColumn`, `@Enumerated`, `@Embedded`, `@Embeddable`).
- Include mapper methods to/from domain models (`from(...)` and `toModel()`) on entities.
- Keep JPA-only concerns in entities; avoid leaking entity types outside persistence adapters.

### Don't

- Don't use records for JPA entities as they require a no-args constructor and mutable fields.
- Don't mix JPA entities with domain models; keep them separate for clear architecture and maintainability.

---

## Example JPA Entity

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users", indexes = {@Index(name = "idx_users_email", columnList = "email")})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    @Column(nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    public static UserEntity from(User user) {
        return UserEntity.builder()
                .id(user.id())
                .email(user.email())
                .name(user.name())
                .status(user.status())
                .createdAt(user.createdAt())
                .updatedAt(user.updatedAt())
                .build();
    }

    public User toModel() {
        return User.builder()
                .id(this.id)
                .email(this.email)
                .name(this.name)
                .status(this.status)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
```
