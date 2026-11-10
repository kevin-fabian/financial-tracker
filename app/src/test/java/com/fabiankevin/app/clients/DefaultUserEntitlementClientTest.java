package com.fabiankevin.app.clients;

import com.fabiankevin.app.clients.dtos.EntitlementClientResponse;
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
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RestClientTest
class DefaultUserEntitlementClientTest {
    @Autowired
    private UserEntitlementClient userEntitlementClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private static final String BASE_URL = "http://localhost:9000/api";

    @TestConfiguration
    static class TestBeanConfiguration {
        @Bean
        public RestClient restClient(RestClient.Builder restClientBuilder) {
            return restClientBuilder.build();
        }

        @Bean
        public UserEntitlementClient userEntitlementClient(RestClient restClient) {
            return new DefaultUserEntitlementClient(restClient,
                    BASE_URL,
                    "financial-tracker-client");
        }
    }

    @Test
    void grantPartnershipEntitlement_validUserId_apiReturns201_returnsEntitlement() {
        UUID userId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        Instant now = Instant.now();

        EntitlementClientResponse expectedResponse = new EntitlementClientResponse(
                entitlementId, userId, "PARTNERSHIP", true, now, null, null, null, now, now);

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/grant-partnership"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

        EntitlementClientResponse result = userEntitlementClient.grantPartnershipEntitlement(userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(entitlementId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.entitlement()).isEqualTo("PARTNERSHIP");
        assertThat(result.active()).isTrue();

        this.mockServer.verify();
    }

    @Test
    void grantPartnershipEntitlement_apiUnreachable_throwsResourceAccessException() {
        UUID userId = UUID.randomUUID();

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/grant-partnership"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                () -> userEntitlementClient.grantPartnershipEntitlement(userId));

        assertThat(exception.getCause()).isInstanceOf(SocketTimeoutException.class);

        this.mockServer.verify();
    }

    @Test
    void grantPartnershipEntitlement_unauthorized_apiReturns401_throwsRuntimeException() {
        UUID userId = UUID.randomUUID();

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/grant-partnership"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> userEntitlementClient.grantPartnershipEntitlement(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");

        this.mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403})
    void grantPartnershipEntitlement_clientError_apiReturns4xx_propagatesHttpClientErrorException(int httpStatus) {
        UUID userId = UUID.randomUUID();

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/grant-partnership"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)));

        assertThatThrownBy(() -> userEntitlementClient.grantPartnershipEntitlement(userId))
                .isInstanceOf(HttpClientErrorException.class);

        this.mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503})
    void grantPartnershipEntitlement_serverError_apiReturns5xx_propagatesHttpServerErrorException(int httpStatus) {
        UUID userId = UUID.randomUUID();

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/grant-partnership"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)));

        assertThatThrownBy(() -> userEntitlementClient.grantPartnershipEntitlement(userId))
                .isInstanceOf(HttpServerErrorException.class);

        this.mockServer.verify();
    }

    @Test
    void revokeEntitlement_validRequest_apiReturns204_succeeds() {
        UUID userId = UUID.randomUUID();
        String entitlement = "PARTNERSHIP";

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.entitlement").value("PARTNERSHIP"))
                .andRespond(withNoContent());

        userEntitlementClient.revokeEntitlement(userId, entitlement);

        this.mockServer.verify();
    }

    @Test
    void revokeEntitlement_apiUnreachable_throwsResourceAccessException() {
        UUID userId = UUID.randomUUID();

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                () -> userEntitlementClient.revokeEntitlement(userId, "PARTNERSHIP"));

        assertThat(exception.getCause()).isInstanceOf(SocketTimeoutException.class);

        this.mockServer.verify();
    }

    @Test
    void revokeEntitlement_unauthorized_apiReturns401_throwsRuntimeException() {
        UUID userId = UUID.randomUUID();

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> userEntitlementClient.revokeEntitlement(userId, "PARTNERSHIP"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");

        this.mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403})
    void revokeEntitlement_clientError_apiReturns4xx_propagatesHttpClientErrorException(int httpStatus) {
        UUID userId = UUID.randomUUID();

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)));

        assertThatThrownBy(() -> userEntitlementClient.revokeEntitlement(userId, "PARTNERSHIP"))
                .isInstanceOf(HttpClientErrorException.class);

        this.mockServer.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503})
    void revokeEntitlement_serverError_apiReturns5xx_propagatesHttpServerErrorException(int httpStatus) {
        UUID userId = UUID.randomUUID();

        this.mockServer.expect(requestTo(BASE_URL + "/users/" + userId + "/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.valueOf(httpStatus)));

        assertThatThrownBy(() -> userEntitlementClient.revokeEntitlement(userId, "PARTNERSHIP"))
                .isInstanceOf(HttpServerErrorException.class);

        this.mockServer.verify();
    }
}
