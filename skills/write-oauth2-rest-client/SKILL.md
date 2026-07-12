---
name: write-oauth2-rest-client
description: Workflow and best practices for calling protected downstream services using spring-boot-starter-oauth2-client with automatic client_credentials token acquisition via RestClient.
---

# OAuth2 Client Credentials REST Client Skill

Primary goal: call protected downstream services by wiring `RestClient` with an `OAuth2AuthorizedClientManager` so the client_credentials grant flow is performed transparently — no manual token fetching or header injection required.

References:
- `app/src/main/java/com/fabiankevin/app/config/RestClientConfig.java`
- `app/src/main/java/com/fabiankevin/app/clients/UserClient.java`
- `app/src/main/java/com/fabiankevin/app/clients/DefaultUserProfileClient.java`
- `app/src/main/resources/application-local.yaml`

## Workflow

1. Add `spring-boot-starter-oauth2-client` dependency to `pom.xml`.
2. Register the downstream service in `application-{profile}.yaml` under `spring.security.oauth2.client.registration` and `spring.security.oauth2.client.provider`.
3. Create the `RestClientConfig` bean that wires `OAuth2AuthorizedClientManager` with `RestClientClientCredentialsTokenResponseClient` and attaches `OAuth2ClientHttpRequestInterceptor` to `RestClient`.
4. Inject `RestClient` into a `@Component` client class; mutate the base URL per downstream service.
5. Write the client interface and implementation — the OAuth2 access token is injected automatically.

---

## Mandatory Rules

- Never fetch or store tokens manually; the `OAuth2AuthorizedClientManager` handles token lifecycle (acquisition, caching, refresh).
- Always use `RestClient` (not `WebClient` or `RestTemplate`) for HTTP calls in this project.
- Keep client interfaces in `clients/` and implementations in `clients/` with `Default*` naming.
- Client DTOs live in `clients/dtos/`, separate from web-layer DTOs.
- Use `restClient.mutate().baseUrl(...).build()` per downstream service — never share a single `RestClient` instance across services with different base URLs.
- Configure connect/read timeouts (30s each) on the underlying `SimpleClientHttpRequestFactory`.
- Set the default `clientRegistrationId` and `principal` on the `RestClient` so the interceptor knows which registration to use.

---

## Practical: Step 1 - Add the dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

---

## Practical: Step 2 - Register the downstream service

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          service2-internal:
            provider: identity
            client-id: zeny-client
            client-secret: super-secret-password
            authorization-grant-type: client_credentials
            scope: api.read
        provider:
          identity:
            issuer-uri: http://localhost:9000
```

Key points:
- `registration.*.authorization-grant-type` must be `client_credentials`.
- `registration.*.scope` lists the API scopes the client needs.
- `provider.*.issuer-uri` points to the OAuth2 issuer; Spring derives the token endpoint automatically.
- `registration.*.client-id` and `client-secret` are the downstream service credentials.

---

## Practical: Step 3 - Wire `RestClient` with OAuth2 interceptor

```java
@Configuration
public class RestClientConfig {
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository,
            OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> accessTokenResponseClient) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials(clientCredentials ->
                                clientCredentials.accessTokenResponseClient(accessTokenResponseClient)
                        )
                        .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> accessTokenResponseClient() {
        return new RestClientClientCredentialsTokenResponseClient();
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder, OAuth2AuthorizedClientManager authorizedClientManager) {
        OAuth2ClientHttpRequestInterceptor requestInterceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        requestInterceptor.setPrincipalResolver(new RequestAttributePrincipalResolver());

        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(Duration.ofSeconds(30));
        simpleClientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(30));

        return builder
                .defaultRequest(requestHeadersSpec -> {
                    requestHeadersSpec.attributes(clientRegistrationId("zeny-client"));
                    requestHeadersSpec.attributes(principal("zeny-client"));
                })
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(requestInterceptor)
                .requestFactory(simpleClientHttpRequestFactory)
                .build();
    }
}
```

How it works:
- `OAuth2AuthorizedClientManager` manages the token lifecycle for all registered clients.
- `RestClientClientCredentialsTokenResponseClient` uses `RestClient` to call the token endpoint.
- `OAuth2ClientHttpRequestInterceptor` intercepts outgoing requests, checks for a valid token, and automatically acquires one via `client_credentials` if missing or expired.
- `clientRegistrationId("zeny-client")` tells the interceptor which registration to use for token acquisition.

---

## Practical: Step 4 - Create the client interface and implementation

```java
public interface UserClient {
    UserResponse register(CreateUserRequest request);
}
```

```java
@Component
public class DefaultUserProfileClient implements UserClient {
    private final RestClient restClient;

    public DefaultUserProfileClient(RestClient restClient) {
        this.restClient = restClient.mutate()
                .baseUrl("http://localhost:9000/api")
                .build();
    }

    @Override
    public UserResponse register(CreateUserRequest request) {
        return restClient.post()
                .uri("/users")
                .body(request)
                .retrieve()
                .toEntity(UserResponse.class)
                .getBody();
    }
}
```

Key points:
- `DefaultUserProfileClient` mutates the shared `RestClient` with a per-service `baseUrl`.
- No `Authorization` header is set manually — the interceptor injects the Bearer token.
- The client interface keeps the implementation swappable for testing.

---

## Practical: Step 5 - Client DTOs

```java
public record CreateUserRequest(
        String firstName,
        String lastName,
        String username,
        String password,
        String confirmPassword) {
}
```

```java
@Builder
public record UserResponse(UUID id, String firstName, String lastName) {
}
```

Guidance:
- Client DTOs are plain records — no `@Schema` annotations needed (not exposed in this API's OpenAPI).
- Keep them in `clients/dtos/` to separate them from web-layer DTOs.

---

## Testing

Use `@WebMvcTest` or `@SpringBootTest` with `@MockitoBean` for the client dependency. For integration tests that actually call the downstream service, use `wiremock` or a test OAuth2 server.

```java
@DataJpaTest
class DefaultUserProfileClientTest {
    @MockitoBean
    private UserClient userClient;

    // ... test service behavior that depends on UserClient
}
```

---

## Checklist

- `spring-boot-starter-oauth2-client` dependency added
- Client registration and provider configured in `application-{profile}.yaml`
- `OAuth2AuthorizedClientManager` bean wired with `clientCredentials` provider
- `RestClient` bean configured with `OAuth2ClientHttpRequestInterceptor`
- Default `clientRegistrationId` and `principal` attributes set on `RestClient`
- Connect/read timeouts configured (30s each)
- Client interface in `clients/` with `Default*` implementation
- Client DTOs in `clients/dtos/`, separate from web-layer DTOs
- `restClient.mutate().baseUrl(...).build()` per downstream service
- No manual token fetching, caching, or header injection in client code
