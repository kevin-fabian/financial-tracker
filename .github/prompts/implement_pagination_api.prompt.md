---
description: 'Implements a practical pagination REST API'
---
## Implement a practical pagination REST API

- Add the specified pagination endpoint to an existing REST controller.
- The pagination endpoint should accept query parameters for page number, page size, sortBy and direction(ASC or DESC).
- The endpoint should return Page<T> where T is the type of the resource being paginated.
- Use JPA paging and sorting capabilities to fetch the paginated data from the database.
- If the following Java records are not existing, create each file for each Java record at:
    - /models
        ```java
        package example.models;
        import java.util.List;
      
        public record Page<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last,
            boolean first
        ) {}
      ```
      ---
      ```java
        package example.models;
      
        public record PageQuery(
            int page,
            int size,
            String sortBy,
            String direction
        ) {}
       ```
    - /web/controllers/dtos
        ```java
        package example.web.controllers.dtos;
        import java.util.List;
      
        public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last,
            boolean first
        ) {}
        ```
- Sample repository method to fetch paginated data:
    ```java
    class DefaultXRepository implements XRepository {
        private final JpaXRepository jpaXRepository;
      
        @Override
        public Page<X> findAll(PageQuery pageQuery) {
            var pageable = PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Direction.fromString(pageQuery.direction()), pageQuery.sortBy())
            );
            var page = jpaXRepository.findAll(pageable);
            return new Page<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
            );
        }
    }
    ```
- Sample `RestController`
    ```java
        @Operation(
            summary = "Retrieve all X with pagination.",
            description = "Retrieves a paginated list of all X.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK - Xs retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PageResponse.class)))
            }
        )
        @GetMapping
        public PageResponse<X> retrieveAll(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size,
                @RequestParam(defaultValue = "createdAt") String sortBy,
                @RequestParam(defaultValue = "DESC") String sortDirection) {
            var pageQuery = new PageQuery(page, size, sortBy, sortDirection);
            var resultPage = service.findAll(pageQuery);
            return new PageResponse<>(
                resultPage.content().stream().map(X::from).toList(),
                resultPage.page(),
                resultPage.size(),
                resultPage.totalElements(),
                resultPage.totalPages(),
                resultPage.last(),
                resultPage.first()
            );
        }
    ```
