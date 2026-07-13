package com.fabiankevin.app.clients;

import com.fabiankevin.app.clients.dtos.UserClientResponse;
import com.fabiankevin.app.exceptions.users.UserNotFoundException;
import com.fabiankevin.app.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RestClientTest
class DefaultUserClientTest {
    @Autowired
    private UserClient userClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String BASE_URL = "http://localhost:1212/api";

    @TestConfiguration
    static class TestBeanConfiguration {
        @Bean
        public RestClient restClient(RestClient.Builder restClientBuilder) {
            return restClientBuilder.build();
        }

        @Bean
        public UserClient userClient(RestClient restClient) {
            return new DefaultUserClient(restClient,
                    BASE_URL,
                    "zeny-service");
        }
    }

    @Test
    void getUserByEmail_validEmail_apiReturns200_returnsUser() {
        UUID expectedId = UUID.randomUUID();
        String expectedEmail = "john@example.com";
        UserClientResponse clientResponse = new UserClientResponse(expectedId, "John", "Doe");

        this.mockServer.expect(requestTo(BASE_URL + "/users?email=" + expectedEmail))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonMapper.writeValueAsString(clientResponse), MediaType.APPLICATION_JSON));

        User result = userClient.getUserByEmail(expectedEmail);

        assertThat(result).isEqualTo(clientResponse.toModel());
        assertThat(result.id()).isEqualTo(expectedId);
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");

        this.mockServer.verify();
    }

    @Test
    void getUserByEmail_apiUnreachable_throwsResourceAccessException() {
        String expectedEmail = "john@example.com";

        this.mockServer.expect(requestTo(BASE_URL + "/users?email=" + expectedEmail))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                () -> userClient.getUserByEmail(expectedEmail));

        assertThat(exception.getCause()).isInstanceOf(SocketTimeoutException.class);

        this.mockServer.verify();
    }

    @Test
    void getUserByEmail_userNotFound_apiReturns404_throwsUserNotFoundException() {
        String expectedEmail = "john@example.com";

        this.mockServer.expect(requestTo(BASE_URL + "/users?email=" + expectedEmail))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> userClient.getUserByEmail(expectedEmail))
                .isInstanceOf(UserNotFoundException.class);

        this.mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    void getUserByEmail_clientError_apiReturns4xx_propagatesHttpClientErrorException(int httpStatus) {
        String expectedEmail = "john@example.com";

        this.mockServer.expect(requestTo(BASE_URL + "/users?email=" + expectedEmail))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)));

        assertThatThrownBy(() -> userClient.getUserByEmail(expectedEmail))
                .isInstanceOf(HttpClientErrorException.class);

        this.mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503})
    void getUserByEmail_serverError_apiReturns5xx_propagatesHttpServerErrorException(int httpStatus) {
        String email = "john@example.com";

        this.mockServer.expect(requestTo(BASE_URL + "/users?email=" + email))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)));

        assertThatThrownBy(() -> userClient.getUserByEmail(email))
                .isInstanceOf(HttpServerErrorException.class);

        this.mockServer.verify();
    }
}
