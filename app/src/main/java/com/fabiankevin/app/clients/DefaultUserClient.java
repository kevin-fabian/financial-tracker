package com.fabiankevin.app.clients;

import com.fabiankevin.app.clients.dtos.CreateUserRequest;
import com.fabiankevin.app.clients.dtos.UserResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DefaultUserClient implements UserClient {
    private final RestClient restClient;

    public DefaultUserClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://localhost:9000/api")
                .build();
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        return restClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(UserResponse.class);
    }
}
