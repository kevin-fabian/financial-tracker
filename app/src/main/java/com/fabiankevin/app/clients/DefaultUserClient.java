package com.fabiankevin.app.clients;

import com.fabiankevin.app.clients.dtos.UserClientResponse;
import com.fabiankevin.app.exceptions.users.UserNotFoundException;
import com.fabiankevin.app.models.User;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;
import static org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver.principal;

public class DefaultUserClient implements UserClient {
    private final RestClient restClient;

    public DefaultUserClient(RestClient restClient, String baseUrl, String clientId) {
        this.restClient = restClient.mutate()
                .baseUrl(baseUrl)
                .defaultRequest(requestHeadersSpec -> {
                    requestHeadersSpec.attributes(clientRegistrationId(clientId));
                    requestHeadersSpec.attributes(principal(clientId));
                })
                .build();
    }

    @Override
    public User getUserByEmail(String email) {
        try {
            UserClientResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/users")
                            .queryParam("email", email)
                            .build())
                    .retrieve()
                    .body(UserClientResponse.class);

            return response.toModel();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                throw new UserNotFoundException(email);
            }
            throw e;
        }
    }
}
