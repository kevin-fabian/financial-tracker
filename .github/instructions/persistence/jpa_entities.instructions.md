---
applyTo: '**/persistence/entities/**/*.java'
description: 'JPA Entity classes'
---

## JPA Entity classes requirements

When writing JPA Entity classes, please follow these guidelines:

- Entity classes should end with `Entity`.
- Entity classes should be placed in the `persistence/entities` package.
- Use class instead of record for entity definitions.
- Each file should contain a single class.
- Each file should use Lombok annotations (e.g., @Data, @Builder, @AllArgsConstructor, @NoArgsConstructor) to reduce boilerplate code.
- No getters and setters should be manually written; use Lombok instead.
- Use JPA annotations for ORM mapping (e.g., @Entity, @Table(name="<table name>"), @Id, @GeneratedValue(strategy = GenerationType.UUID), @Column).
- Use `@Enumerated(EnumType.STRING)` for enum fields.
- Use `@Embedded` for embedded objects. If the embedded class does not exist, create it in the `persistence/entities/embeddables` package.`.
- Use `@Column` to specify column details and constraints.
- Use `@ManyToOne`, `@OneToMany`, `@OneToOne`, and `@JoinColumn` for relationships.
- Use `@Lob` for large text or binary fields.
- Include standard fields like `id`, `createdAt`, and `updatedAt`.
- Include utility methods to convert toModel()/from domain model objects.
- Use Java Instant for timestamp fields. e.g., createdAt, updatedAt.
- Use Java LocalDate for dates. e.g., birthDate
- Use Lombok builder when mapping entity to model and vice versa.
- The interface should be concise and focused on JPA-specific operations only.

Examples:

```java

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    public UserEntity from(User user) {
        return UserEntity.builder()
                .id(user.id())
                .createdAt(user.createdAt())
                .updatedAt(user.updatedAt())
                .build();
    }

    public User toModel() {
        return User.builder()
                .id(this.id)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
```