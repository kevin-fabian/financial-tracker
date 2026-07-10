# Write REST Client Tests

Primary goal: write tests for Spring REST client implementations (`RestClient`-based) that verify HTTP contract correctness — request shape, response mapping, and error handling — using `@RestClientTest` with `MockRestServiceServer`.

## Workflow

1. Read the client interface and implementation to understand the base URL, HTTP method, URI path, content type, request body, and response type.
2. Write a maximum of 3 test scenarios per client method using a black-box approach.
3. Cover: successful response, client error (4xx), and network failure (timeout/unreachable).
4. Consolidate repetitive HTTP error codes into a single `@ParameterizedTest`.
5. Verify the implementation against the tests and fix bugs if it fails.

## Required Dependencies

Both dependencies must be present in `pom.xml` for `@RestClientTest` to work:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-restclient</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-restclient-test</artifactId>
    <scope>test</scope>
</dependency>
```

The first provides the production `RestClient` support; the second provides `@RestClientTest` and `MockRestServiceServer` for testing.

## Mandatory Rules

- Use `@RestClientTest(ClientClass.class)` to slice the REST client configuration.
- `@Autowired` both the client under test and `MockRestServiceServer`.
- Use `JsonMapper` (from `tools.jackson.databind.json.JsonMapper`) to serialize request bodies for `content().json(...)` assertions.
- Use `requestTo(fullUrl)` — include the complete base URL as configured on the `RestClient`.
- Assert request shape: HTTP method, `Content-Type` header, and serialized body.
- Assert response shape: status code, headers, and JSON body mapped to the response DTO.
- Use `MockRestResponseCreators` — `withSuccess()`, `withBadRequest()`, `withServerError()`, `withException()`.
- Use `@ValueSource` to parameterize multiple 4xx/5xx status codes into one test.
- Use `ResourceAccessException` + `SocketTimeoutException` for network failure scenarios.
- Call `mockServer.verify()` at the end of every test to ensure all expected requests were consumed.
- Use AssertJ for all assertions (`assertThat`).
- Name tests: `methodName_givenScenario_whenAction_thenOutcome`.
- No comments inside test methods.
- Keep blank-line separation between Arrange, Act, and Assert blocks.

## Test Scenarios to Cover

| Scenario | Mock Response | Expected Exception / Result |
|---|---|---|
| Happy path | `withSuccess(json, APPLICATION_JSON)` | Mapped response DTO |
| Client error | `withStatus(HttpStatus.BAD_REQUEST)` | `HttpClientErrorException` |
| Server error | `withStatus(HttpStatus.INTERNAL_SERVER_ERROR)` | `HttpServerErrorException` |
| Network failure | `withException(new SocketTimeoutException(...))` | `ResourceAccessException` |

## Parameterizing HTTP Errors

Replace multiple individual error tests with one parameterized test:

```java
@ParameterizedTest
@ValueSource(ints = {400, 401, 403, 404})
void methodName_invalidInput_apiReturns4xx_throwsHttpClientErrorException(int httpStatus) {
    // Arrange
    this.mockServer.expect(requestTo("http://localhost:8080/api/endpoint"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.valueOf(httpStatus)));

    // Act & Assert
    HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> client.methodName(request));

    assertThat(exception.getStatusCode().is4xxClientError()).isTrue();

    this.mockServer.verify();
}
```

## Full Example

```java
@RestClientTest(DefaultUserClient.class)
class DefaultUserClientTest {
    @Autowired
    private DefaultUserClient userClient;

    @Autowired
    private MockRestServiceServer mockServer;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void createUser_validRequest_apiReturns200_returnsUserResponse() {
        UUID expectedId = UUID.randomUUID();
        String mockJsonResponse = """
                {"id": "%s", "firstName": "John", "lastName": "Doe"}
                """.formatted(expectedId);

        CreateUserRequest request = new CreateUserRequest("John", "Doe", "user", "pass", "pass");

        this.mockServer.expect(requestTo("http://localhost:9000/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(jsonMapper.writeValueAsString(request)))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        UserResponse result = userClient.createUser(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(expectedId);
        assertThat(result.firstName()).isEqualTo("John");

        this.mockServer.verify();
    }

    @Test
    void createUser_apiUnreachable_throwsResourceAccessException() {
        CreateUserRequest request = new CreateUserRequest("John", "Doe", "user", "pass", "pass");

        this.mockServer.expect(requestTo("http://localhost:9000/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                () -> userClient.createUser(request));

        assertThat(exception.getCause()).isInstanceOf(SocketTimeoutException.class);

        this.mockServer.verify();
    }
}
```

## Checklist

- used `@RestClientTest(ClientClass.class)`
- autowired the client and `MockRestServiceServer`
- used `JsonMapper` to serialize request bodies for assertion
- asserted request shape (method, headers, body)
- asserted response mapping (DTO fields, status)
- covered success, client error, and network failure
- used `@ValueSource` to parameterize repetitive error codes
- used `MockRestResponseCreators` for all mock responses
- called `mockServer.verify()` in every test
- used AssertJ for all assertions
- test names follow `methodName_given_when_then`
- no comments inside test methods
- blank-line separation between Arrange/Act/Assert blocks
