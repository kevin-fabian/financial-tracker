---
name: light-hexagonal-architecture
description: Decision framework and patterns for light hexagonal architecture — domain models stay framework-agnostic while infrastructure concerns (caching, persistence, external calls) live in adapters. Use when deciding where a class belongs.
---

# Light Hexagonal Architecture Skill

Primary goal: keep domain logic free from framework coupling so business rules can be understood, tested, and evolved independently — while still allowing pragmatic Spring Boot and Lombok usage where it reduces boilerplate without coupling.

References:
- `.github/skills/write-domain-services/SKILL.md`
- `.github/skills/write-jpa-domain-repositories/SKILL.md`

---

## Core Principle

In **light hexagonal architecture**, the domain layer is the stable core. It defines *what* the business does (rules, invariants, orchestration) without depending on *how* external systems fulfill those needs (databases, caches, HTTP clients, message brokers).

The outer infrastructure layer implements ports defined by the domain and may use Spring Boot, caching, JPA, and other frameworks freely. Cross-cutting concerns like caching are **mechanisms**, not **policies**, and must never live inside the domain.

---

## Two Mental Tests

Use these tests whenever you're unsure where a class belongs.

### Test 1: "Delete the Framework"

> If you uninstalled Spring Boot, Redis, and JPA tomorrow and rewrote the outer layer in vanilla Java or a lightweight framework, would this class still be required to make correct business decisions?

| Class | Result | Layer |
|-------|--------|-------|
| `DefaultUserService` (validates, enforces rules) | Yes — the business still needs this logic. | **Domain** |
| `CachedUserService` (delegates to Redis on miss) | No — the app would still be correct, just slower. | **Infrastructure** |
| `JpaUserRepository` (executes SQL) | No — persistence is an external I/O detail. | **Infrastructure** |
| `UserEntity` (JPA-mapped table row) | No — a domain model is enough without it. | **Infrastructure** |

### Test 2: "Policy vs. Mechanism"

| Question | Answer | Layer |
|----------|--------|-------|
| Does it encode a business rule that changes when the business changes? | Yes | **Domain** |
| Does it optimize *how fast*, *how safely*, or *how reliably* a rule executes? | Yes | **Infrastructure** |

---

## Architectural Decision Table

Use this table to classify common scenarios by their correct layer and framework-dependency tolerance.

| Scenario / Characteristic | Layer | Framework Dependent? | Practical Reason |
|---|---|---|---|
| **Business invariants & rules** (discount calculation, age validation, activation gating) | **Domain** | **No** — framework-agnostic | Business rules must change only when the business changes, not when a library is upgraded. |
| **Orchestration & coordination** (composite services, multi-step flows, saga-like sequences that coordinate domain collaborators) | **Domain** | **No** — or minimal (stereotype annotations only) | The step-by-step sequencing of internal business operations *is* the domain logic. |
| **Performance optimizations** (caching, connection pooling, async/parallel blocks) | **Infrastructure** | **Yes** | These are technical adjustments to run efficiently on hardware; correctness does not depend on them. |
| **Security mechanisms** (BCrypt encoding, JWT parsing, OAuth state validation) | **Infrastructure** (as adapters) | **Yes** | Cipher standards and security libraries change frequently; isolate them so patches don't force domain re-tests. |
| **Data persistence & queries** (JPA entities, Spring Data repos, SQL strings) | **Infrastructure** | **Yes** | Databases are external I/O instruments. The domain defines *what* data it needs via a port interface; the infrastructure implements it. |
| **External protocols & APIs** (RestClient, AWS SDK, Kafka producers) | **Infrastructure** | **Yes** | Third-party networks are unreliable and external; adapt them into domain types via adapters. |
| **Cross-cutting operational wrappers** (caching decorators, circuit breakers, retry proxies) | **Infrastructure** | **Yes** | They wrap pure domain services without altering business semantics. |

---

## The Caching Proxy Pattern

Caching is the canonical cross-cutting concern. It must live in the infrastructure layer as an **operational decorator (proxy)** around a pure domain service.

### Structure

```
Domain Layer (pure)                    Infrastructure Layer (framework-dependent)
┌─────────────────────┐                ┌────────────────────────────────────────┐
│  UserService        │◄───────────────│  CachedUserService  (@Primary)         │
│  (interface)        │   delegates to │   implements UserService               │
│                     │                │   wraps DefaultUserService              │
│  DefaultUserService │────────────────┘   adds @Cacheable / Redis / etc.       │
│  (business logic)   │                                                  │
└─────────────────────┘                                                  │
                                                                         │
┌─────────────────────┐                                                  │
│  UserRepository     │                                                  │
│  (port interface)   │                                                  │
└─────────────────────┘                                                  │
                                                                         │
┌─────────────────────┐                ┌────────────────────────────────────────┐
│  JpaUserRepository  │────────────────│  implements UserRepository             │
│  (adapter)          │                │   uses Spring Data JPA                 │
└─────────────────────┘                └────────────────────────────────────────┘
```

### Step 1: Define the pure domain contract and implementation

```java
package com.example.domain.user;

public interface UserService {
    User getUserById(Long id);
}

public class DefaultUserService implements UserService {
    private final UserRepository userRepository;

    public DefaultUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

Rules:
- `UserService` is a plain interface — no framework types.
- `DefaultUserService` contains only business logic. May use Lombok (`@RequiredArgsConstructor`) for boilerplate but must not depend on Spring caching, HTTP, or persistence APIs.
- Throws domain exceptions only (`UserNotFoundException`).

### Step 2: Create the caching proxy in the infrastructure layer

```java
package com.example.infrastructure.cache;

import com.example.domain.user.User;
import com.example.domain.user.UserService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CachedUserService implements UserService {

    private final UserService delegate;

    public CachedUserService(UserService defaultUserService) {
        this.delegate = defaultUserService;
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return delegate.getUserById(id);
    }
}
```

Rules:
- `@Component @Primary` — Spring injects this proxy everywhere `UserService` is requested.
- `@Cacheable` couples the method to Spring's caching infrastructure (Redis, Caffeine, etc.).
- On cache miss, delegates to the real domain service — the proxy adds no business logic.
- The proxy implements the *same domain interface*, so callers remain decoupled from caching.

### Step 3: Wire in configuration

```java
package com.example.config;

import com.example.domain.user.DefaultUserService;
import com.example.domain.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    @Bean
    public UserService defaultUserService(UserRepository userRepository) {
        return new DefaultUserService(userRepository);
    }
}
```

Why manual wiring? `CachedUserService` needs to inject the concrete `DefaultUserService` (not any `UserService` bean, or it would inject itself). Manual configuration makes the delegation chain explicit and avoids circular ambiguity.

---

## What's Allowed in the Domain (Light vs. Pure)

This architecture is **light** hexagonal, not pure hexagonal. The domain may use:

| Allowed | Not Allowed |
|---------|-------------|
| Lombok (`@Builder`, `@RequiredArgsConstructor`, `@Getter`, `@ToString`) | Spring caching (`@Cacheable`, `@CacheEvict`) |
| Java standard library (`java.time`, `java.util`, `java.math`) | Spring HTTP (`RestClient`, `WebClient`, `HttpServletRequest`) |
| Domain-only records and interfaces | JPA annotations (`@Entity`, `@Table`, `@Column`) |
| Stereotype annotations on infra-only classes | Direct dependency on external SDKs (AWS, Kafka producer APIs) |
| `@Service` on `Default...Service` only when needed for component scanning (prefer manual `@Bean` wiring for testability) | Logging frameworks other than SLF4J facade (and even then, keep it minimal) |

The key distinction: **Lombok generates code at compile time and has no runtime framework dependency**. Spring caching, JPA, and HTTP clients couple your domain to a runtime container — that coupling belongs in infrastructure.

### Explicit Cache Annotation Exclusions

The following Spring caching annotations are **strictly excluded** from the domain layer. They must only appear inside infrastructure-layer proxies (e.g., `CachedUserService`), never on domain service methods or domain models:

| Annotation | Purpose | Exclusion Reason |
|---|---|---|
| `@Cacheable` | Declarative cache lookup/population | Couples business logic to a runtime caching proxy; the domain doesn't care whether a result came from cache or computation. |
| `@CacheEvict` | Declarative cache invalidation | Ties domain write operations to cache lifecycle; invalidation strategy is an infrastructure concern. |
| `@CachePut` | Declarative cache update | Makes domain methods responsible for cache consistency, leaking infrastructure state management into business logic. |
| `@Caching` | Grouping multiple cache operations | Composite of the above — all exclusions apply simultaneously. |
| `@CacheConfig` | Class-level cache configuration | Applies cache semantics at the class level, which would silently couple an entire domain service to caching infrastructure. |

**Rule of thumb:** if a method or class carries any `org.springframework.cache.annotation.*` annotation, it does not belong in the domain layer. Move it to a `Cached...` proxy in infrastructure.

---

## Decision Checklist

Use this checklist when placing a new class:

- [ ] **Does it express a business rule that would change if the business changed?** → Domain, framework-agnostic.
- [ ] **Does it optimize performance, resilience, or external communication?** → Infrastructure, framework is fine.
- [ ] **Can it be removed without breaking business correctness (only speed/reliability suffers)?** → Infrastructure (proxy/decorator).
- [ ] **Does it implement a domain interface while adding a cross-cutting behavior?** → Infrastructure proxy with `@Primary`.
- [ ] **Would you need to mock a framework cache/SQL/HTTP client to unit test it?** — If yes, and the class is supposed to be domain, it's misplaced.

---

## Anti-Patterns to Avoid

| Anti-Pattern | Problem | Fix |
|---|---|---|
| `@Cacheable` on a domain service method | Couples business logic to Spring caching runtime | Move caching to a `Cached...Service` proxy in infrastructure. |
| Domain model imports `jakarta.persistence.*` | Leaks persistence into the core | Use a JPA entity in the infrastructure layer; convert via `from(model)` / `toModel()`. |
| Domain service accepts `HttpServletRequest` to read a header | Couples domain to the web layer | Extract the value in the controller and pass it as a command/query parameter. |
| Orchestration flow lives inside a repository adapter | Hides business sequencing behind a persistence abstraction | Move orchestration to a domain service; keep repositories thin. |
| `Default...Service` depends on a concrete infrastructure class | Inverts the dependency direction | Depend on a domain-defined port interface; let infrastructure implement it. |

---

## Testing Implications

| Layer | Test Approach | Framework Needed |
|---|---|---|
| `DefaultUserService` | Pure unit test with mocked `UserRepository` | None (or just JUnit + Mockito) |
| `CachedUserService` | Verify delegation behavior; cache behavior is Spring's concern | Spring caching test context (only if testing the proxy itself, which is rare) |
| `JpaUserRepository` | `@DataJpaTest` with real or in-memory datasource | Spring Boot test slice |

Domain tests should run without a Spring context — that's the payoff of keeping the core framework-free.
