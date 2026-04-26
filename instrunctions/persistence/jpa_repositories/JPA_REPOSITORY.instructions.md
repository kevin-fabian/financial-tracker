---
applyTo: '**/Jpa*.java'
description: 'Guidelines for writing JPA repository interfaces in this repository'
---

## Writing JPA Repository Interfaces Best Practices

### Do

- Name the interface starting with `Jpa` + entity name + `Repository` (e.g., `JpaUserRepository`).
- Use `UUID` as the identifier type for entities with UUID primary keys.
- Extend the interface from `JpaRepository<T, ID>`. e.g., `public interface JpaUserRepository extends JpaRepository<UserEntity, UUID>`.
- Add `@EntityGraph` where necessary to optimize fetching related entities. e.g., `@EntityGraph(attributePaths = {"relatedEntity"})` on methods that require eager loading of relationships.

---

## Example JPA Repository Interface

```java
interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
    @EntityGraph(attributePaths = {"credentials", "roles"})
    Optional<UserEntity> findByEmail(String email);
}
```
