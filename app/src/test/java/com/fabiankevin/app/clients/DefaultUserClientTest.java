package com.fabiankevin.app.clients;

import com.fabiankevin.app.models.User;
import com.fabiankevin.app.services.commands.CreateUserCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.json.JsonMapper;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

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
                {
                    "id": "%s",
                    "firstName": "John",
                    "lastName": "Doe"
                }
                """.formatted(expectedId);

        CreateUserCommand request = CreateUserCommand.builder()
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .password("password123")
                .confirmPassword("password123")
                .build();

        this.mockServer.expect(requestTo("http://localhost:9000/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(jsonMapper.writeValueAsString(request)))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        User result = userClient.createUser(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(expectedId);
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");

        this.mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404})
    void createUser_invalidRequest_throwsHttpClientErrorException(int httpStatus) {
        CreateUserCommand request = CreateUserCommand.builder()
                .firstName("")
                .lastName("")
                .username("")
                .password("")
                .confirmPassword("")
                .build();

        this.mockServer.expect(requestTo("http://localhost:9000/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)));

        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> userClient.createUser(request));

        assertThat(exception.getStatusCode().is4xxClientError()).isTrue();

        this.mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503})
    void createUser_serverError_throwsHttpServerErrorException(int httpStatus) {
        CreateUserCommand request = CreateUserCommand.builder()
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .password("password123")
                .confirmPassword("password123")
                .build();

        this.mockServer.expect(requestTo("http://localhost:9000/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)));

        HttpServerErrorException exception = assertThrows(
                HttpServerErrorException.class,
                () -> userClient.createUser(request));

        assertThat(exception.getStatusCode().is5xxServerError()).isTrue();

        this.mockServer.verify();
    }

    @Test
    void createUser_apiUnreachable_throwsResourceAccessException() {
        CreateUserCommand request = CreateUserCommand.builder()
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .password("password123")
                .confirmPassword("password123")
                .build();

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
