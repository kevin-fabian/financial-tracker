package com.fabiankevin.app.clients;

import com.fabiankevin.app.clients.dtos.CreateUserClientRequest;
import com.fabiankevin.app.exceptions.users.UsernameAlreadyTakenException;
import com.fabiankevin.app.models.User;
import com.fabiankevin.app.services.commands.CreateUserCommand;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
public class DefaultUserClient implements UserClient {
    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public DefaultUserClient(RestClient.Builder restClientBuilder, JsonMapper jsonMapper) {
        this.restClient = restClientBuilder
                .baseUrl("http://localhost:9000/api")
                .build();
        this.jsonMapper = jsonMapper;
    }

    @Override
    public User createUser(CreateUserCommand command) {
        return restClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(CreateUserClientRequest.from(command))
                .retrieve()
                .onStatus((response) -> {
                    if (response.getStatusCode().is4xxClientError()) {
                        handleClientError(response);
                        return true;
                    }
                    return false;
                })
                .body(User.class);
    }

    private void handleClientError(ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        if (statusCode.is4xxClientError()) {
            String errorBody = readBody(response);
            ProblemDetail problemDetail = jsonMapper.readValue(errorBody, ProblemDetail.class);
            if (statusCode.value() == 409) {
                throw new UsernameAlreadyTakenException("");
            }
            throw new RuntimeException("");
        }
    }

    private String readBody(ClientHttpResponse response) throws IOException {
        return new String(response.getBody().readAllBytes());
    }
}
