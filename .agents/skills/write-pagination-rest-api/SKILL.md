---
name: write-pagination-rest-api
description: Workflow and best practices for implementing paginated REST endpoints with PageQuery, Page, and PageResponse across controller, service, domain repository, and JPA repository layers.
---

# Pagination REST API Best-Practice Skill

Primary goal: implement paginated list endpoints that follow the established four-layer pagination chain — controller extracts `PageQuery`, service passes it through, domain repository converts to Spring `Pageable`, and JPA repository returns a Spring `Page` that gets wrapped into the domain `Page` record.

References:
- `.github/skills/write-rest-api/SKILL.md`
- `.github/skills/write-domain-services/SKILL.md`
- `.github/skills/write-jpa-domain-repositories/SKILL.md`
- `.github/skills/write-test-cases/SKILL.md`

## The Pagination Chain

```
HTTP GET /api/resources?page=0&size=10&sort=createdAt&direction=DESC
    |
    v
Controller.get...()
    - @RequestParam with defaults: page=0, size=10, sort=createdAt, direction=DESC
    - Extracts userId from JwtAuthenticationToken
    - Constructs new PageQuery(page, size, sort, direction)
    - Calls service.get...ByPageQuery(query, userId, optionalFilters)
    - Maps Page<Domain> -> PageResponse<ResponseDTO> via PageResponse.from()
    |
    v
Service interface
    - Returns Page<DomainModel>
    - Signature: Page<Xxx> get...ByPageQuery(PageQuery query, UUID userId, ...optional filters)
    |
    v
DefaultService (thin pass-through)
    - Forwards PageQuery directly to repository
    |
    v
Domain repository interface
    - Returns Page<DomainModel>
    - Signature: Page<Xxx> findAllByPageQuery(PageQuery query, UUID userId, ...optional filters)
    |
    v
DefaultRepository (PageQuery -> Pageable conversion)
    - Builds PageRequest.of(query.page(), query.size(), Sort.by(...))
    - Calls JPA repository with Pageable
    - Maps Page<Entity> -> Page<Domain> via .map(Entity::toModel)
    - Wraps in domain Page<T> record
    |
    v
JPA repository (Spring Data JPA)
    - Derived method: Page<Entity> findAllByUserId(UUID userId, Pageable pageable)
    - Custom JPQL for complex filters: @Query + Pageable
```

---

## Mandatory Rules

- Always use the four established pagination types: `PageQuery` (input), `Page` (domain model), `PageRequest` (web DTO), `PageResponse` (web DTO).
- Controller `@RequestParam` defaults must be: `page=0`, `size=10`, `sort=createdAt`, `direction=DESC`.
- Service methods for paginated lists must accept `PageQuery` and return `Page<DomainModel>`.
- Domain repository methods must accept `PageQuery` and return `Page<DomainModel>`.
- `DefaultRepository` is the only place that converts `PageQuery` to Spring's `PageRequest` and wraps the result in the domain `Page` record.
- JPA repository methods must accept `Pageable` and return Spring's `Page<Entity>`.
- Cross-user isolation: every JPA query must be scoped to `user` — the user can never see another user's data.
- Never accept `page`, `size`, `sort`, or `direction` parameters directly in service or domain repository contracts — always use `PageQuery`.
- Never expose Spring's `Page<Entity>` or `Pageable` outside the persistence layer.
- Always map entity pages to domain pages via `.map(EntityEntity::toModel)` before wrapping in `Page<T>`.

---

## Practical: Step 1 - Controller with paginated endpoint

```java
@Operation(
    summary = "Retrieves paginated resources",
    description = "Retrieves a paginated list of resources based on the provided pagination parameters",
    responses = {
        @ApiResponse(responseCode = "200", description = "OK - Resources retrieved successfully",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found - Resource not found"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error - Service failure")
    }
)
@GetMapping
public PageResponse<ResourceResponse> getResources(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "createdAt") String sort,
    @RequestParam(defaultValue = "DESC") String direction,
    @RequestParam(required = false) SomeFilter filter,
    JwtAuthenticationToken jwtAuthenticationToken) {
    UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
    Page<Resource> resources = resourceService.getResourcesByPageQuery(
        new PageQuery(page, size, sort, direction), userId, filter);

    return PageResponse.from(Page.<ResourceResponse>builder()
        .content(resources.content().stream().map(ResourceResponse::from).toList())
        .page(resources.page())
        .size(resources.size())
        .totalElements(resources.totalElements())
        .totalPages(resources.totalPages())
        .last(resources.last())
        .first(resources.first())
        .build());
}
```

Guidance:
- Extract pagination parameters from `@RequestParam` with the standard defaults.
- Extract `user` from the JWT token — never from request payloads.
- Build `PageQuery` inline and pass it to the service.
- Map the domain `Page<T>` to `PageResponse<T>` using `PageResponse.from()` and stream the content through the response DTO mapper.
- Use `Page.<ResourceResponse>builder()` generic syntax to reuse the `Page` builder for the response shape.

---

## Practical: Step 2 - Service interface and implementation

**Interface:**
```java
public interface ResourceService {
    Page<Resource> getResourcesByPageQuery(PageQuery query, UUID userId, SomeFilter filter);
}
```

**Implementation:**
```java
@Service
@RequiredArgsConstructor
public class DefaultResourceService implements ResourceService {
    private final ResourceRepository resourceRepository;

    @Override
    public Page<Resource> getResourcesByPageQuery(PageQuery query, UUID userId, SomeFilter filter) {
        return resourceRepository.findAllByPageQuery(query, userId, filter);
    }
}
```

Guidance:
- Service methods for paginated lists must accept `PageQuery` (not individual pagination parameters).
- The service layer is a thin pass-through for pagination — it does not transform pagination parameters.
- Return `Page<DomainModel>`, never Spring's `Page<Entity>` or web DTOs.
- Add business logic (validation, caching, orchestration) around the repository call when needed.

---

## Practical: Step 3 - Domain repository interface

```java
public interface ResourceRepository {
    Page<Resource> findAllByPageQuery(PageQuery query, UUID userId, SomeFilter filter);
}
```

Guidance:
- Domain repository methods for pagination must accept `PageQuery` and return `Page<DomainModel>`.
- No Spring imports or annotations in the interface.
- Name the method consistently: `findAllByPageQuery` or `get...ByPageQuery`.

---

## Practical: Step 4 - DefaultRepository (PageQuery -> Pageable conversion)

```java
@Repository
@RequiredArgsConstructor
public class DefaultResourceRepository implements ResourceRepository {
    private final JpaResourceRepository jpaResourceRepository;

    @Override
    public Page<Resource> findAllByPageQuery(PageQuery query, UUID userId, SomeFilter filter) {
        var pageable = PageRequest.of(
            query.page(),
            query.size(),
            Sort.by(Sort.Direction.fromString(query.direction()), query.sort())
        );

        Page<Resource> result = Optional.ofNullable(filter)
            .map(f -> jpaResourceRepository.findAllByUserIdAndFilter(userId, f, pageable)
                .map(ResourceEntity::toModel))
            .orElseGet(() -> jpaResourceRepository.findAllByUserId(userId, pageable)
                .map(ResourceEntity::toModel));

        return new Page<>(
            result.getContent(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.isLast(),
            result.isFirst()
        );
    }
}
```

Guidance:
- Build `PageRequest` from `PageQuery` using `PageRequest.of(page, size, Sort.by(...))`.
- Parse sort direction via `Sort.Direction.fromString(query.direction())`.
- Use `Optional.ofNullable(filter)` for branching on optional filter parameters (consistent with the Category pattern).
- Always map entity pages to domain models via `.map(ResourceEntity::toModel)`.
- Wrap the Spring `Page<Entity>` into the domain `Page<T>` record using the constructor.
- Cross-user isolation: every JPA call must include `user` in the query parameters.

---

## Practical: Step 5 - JPA repository

**Simple derived query (no extra filters):**
```java
public interface JpaResourceRepository extends JpaRepository<ResourceEntity, UUID> {
    Page<ResourceEntity> findAllByUserId(UUID userId, Pageable pageable);
}
```

**With optional filter (derived method name):**
```java
public interface JpaResourceRepository extends JpaRepository<ResourceEntity, UUID> {
    Page<ResourceEntity> findAllByUserId(UUID userId, Pageable pageable);
    Page<ResourceEntity> findAllByUserIdAndFilter(UUID userId, SomeFilter filter, Pageable pageable);
}
```

**With complex filter requiring JOIN or custom logic:**
```java
public interface JpaResourceRepository extends JpaRepository<ResourceEntity, UUID> {
    @Query("""
        SELECT r FROM ResourceEntity r
        WHERE r.account.user = :user
          AND (:filter IS NULL OR r.someField = :filter)
        """)
    Page<ResourceEntity> findAllByUserIdAndFilter(
        @Param("user") UUID userId,
        @Param("filter") SomeFilter filter,
        Pageable pageable);
}
```

Guidance:
- Always scope queries to `user` for cross-user isolation.
- Use Spring Data derived queries for simple equality filters.
- Use `@Query` with JPQL for complex filters involving JOINs or nested relationships.
- Always return Spring's `Page<Entity>` — never the domain model at this layer.
- Name methods consistently: `findAllByUserId` + optional `And...` suffix for filters.

---

## Practical: Step 6 - Tests

**Controller test (`@WebMvcTest`):**
```java
@WebMvcTest(ResourceController.class)
class ResourceControllerTest {

    @MockitoBean
    private ResourceService resourceService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getResources_withValidParams_returnsPaginatedResponse() throws Exception {
        Page<Resource> mockPage = Page.<Resource>builder()
            .content(List.of(Resource.builder().id(UUID.randomUUID()).build()))
            .page(0)
            .size(10)
            .totalElements(1L)
            .totalPages(1)
            .last(true)
            .first(true)
            .build();

        when(resourceService.getResourcesByPageQuery(any(PageQuery.class), any(UUID.class), isNull()))
            .thenReturn(mockPage);

        mockMvc.perform(get("/api/resources")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt")
                .param("direction", "DESC")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(1));
    }
}
```

**Repository test (`@DataJpaTest`):**
```java
@DataJpaTest
class DefaultResourceRepositoryTest {

    @MockitoSpyBean
    private JpaResourceRepository jpaResourceRepository;

    @Autowired
    private ResourceRepository repository;

    @TestConfiguration
    static class ContextConfiguration {
        @Bean
        ResourceRepository resourceRepository(JpaResourceRepository jpaResourceRepository) {
            return new DefaultResourceRepository(jpaResourceRepository);
        }
    }

    @Test
    void findAllByPageQuery_returnsMappedDomainPage() {
        PageQuery query = new PageQuery(0, 10, "createdAt", "DESC");
        UUID userId = UUID.randomUUID();

        Page<ResourceEntity> entityPage = Page.empty();
        when(jpaResourceRepository.findAllByUserId(userId, Pageable.unpaged()))
            .thenReturn(entityPage);

        Page<Resource> result = repository.findAllByPageQuery(query, userId, null);

        assertNotNull(result, "Result Page should not be null");
        assertEquals(0, result.totalElements(), "Total elements should match");
        verify(jpaResourceRepository, times(1)).findAllByUserId(eq(userId), any(Pageable.class));
    }
}
```

Guidance:
- Controller tests use `@WebMvcTest` with `@MockitoBean` for the service and `MockMvc` for HTTP assertions.
- Repository tests use `@DataJpaTest` with a nested `@TestConfiguration` to register the adapter bean.
- Verify that `PageQuery` is correctly converted to `Pageable` and that entity-to-domain mapping occurs.
- Test both the filtered and unfiltered repository paths when optional filters exist.

---

## Checklist

- controller extracts pagination params with standard defaults (page=0, size=10, sort=createdAt, direction=DESC)
- controller builds `PageQuery` and passes it to service
- service interface accepts `PageQuery` and returns `Page<DomainModel>`
- service implementation is a thin pass-through to the repository
- domain repository interface accepts `PageQuery` and returns `Page<DomainModel>`
- `DefaultRepository` converts `PageQuery` to `PageRequest` with `Sort.by(...)`
- `DefaultRepository` maps `Page<Entity>` to domain `Page<T>` via constructor
- JPA repository accepts `Pageable` and returns Spring's `Page<Entity>`
- cross-user isolation enforced at JPA query level via `user`
- `PageResponse.from()` used in controller to map domain `Page` to web response
- controller tests use `@WebMvcTest` with `MockMvc` and `@MockitoBean`
- repository tests use `@DataJpaTest` with nested `@TestConfiguration`
- no Spring framework types leak outside the persistence layer
- no individual pagination parameters in service or domain repository contracts
