---
applyTo: '**/persistence/repositories/**/*Repository.java'
description: 'Core repository interfaces and implementations' 
---

## Repository Interface Implementation Requirements

When writing Core Repositories, please follow these guidelines:

 - Place repository interfaces in the `persistence/repositories` package.
 - Each file should contain a single interface or class.
 - Name the interface or class ending with `Repository`.
 - Every method return type should be a domain model or a collection of domain models. e.g., `User`, `List<User>`, `Optional<User>`, etc.
 - The interface or class should be concise and focused on repository-specific operations only.
 - Use custom exceptions where necessary to handle error scenarios (e.g., EntityNotFoundException).
 - Use `@Repository` annotation for implementation concrete classes.
 - Use `@RequiredArgsConstructor` from Lombok for implementation classes to handle dependency injection.
 - Inject the corresponding JPA repository interface to perform database operations in the implementation class.

Examples:

## Core Repository Interface
```java
package com.example.persistence.repositories;

public interface UserRepository {
    Optional<User> findById(String id);
    User save(User user);
    void deleteById(String id);
}
```

## Core Repository Interface Implementation
```java
package com.example.persistence.repositories;

import com.example.persistence.jpa_repositories.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class DefaultUserRepository implements UserRepository {
    private final JpaUserRepository jpaUserRepository;
    
    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id)
                .map(UserEntity::toModel);
    }

    @Override
    public User save(User user) {
        return jpaUserRepository.save(UserEntity.from(user)).toModel();
    }
    
    @Override
    void deleteById(String id) {
        jpaUserRepository.deleteById(id);
    }
}
```
