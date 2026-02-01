---
applyTo: '**/persistence/jpa_repositories/**/Jpa*.java'
description: 'JPA Repository interfaces'
---

## JPA Repository Interface requirements

When writing Spring JPA Repository interface, please follow these guidelines:

- Place JPA repository interfaces in the `persistence/jpa_repositories` package.
- Each file should contain a single interface.
- Name the interface starting with `Jpa` followed by the entity name and ending with `Repository
- Use UUID as the identifier type for entities with UUID primary keys.
- Extend the interface from `JpaRepository<T, ID>`.
- Add `@EntityGraph` where necessary to optimize fetching related entities.
- The interface should be concise and focused on JPA-specific operations only.

Examples:

```java
package com.example.persistence.jpa_repositories;

import com.example.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
}
```
