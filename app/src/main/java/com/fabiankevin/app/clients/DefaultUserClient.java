package com.fabiankevin.app.clients;

import com.fabiankevin.app.clients.dtos.UserClientResponse;
import com.fabiankevin.app.models.User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;
import static org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver.principal;

@Component
public class DefaultUserClient implements UserClient {
    private final RestClient restClient;

    public DefaultUserClient(RestClient restClient) {
        this.restClient = restClient.mutate()
                .baseUrl("http://localhost:9000/api")
                .defaultRequest(requestHeadersSpec -> {
                    requestHeadersSpec.attributes(clientRegistrationId("zeny-client"));
                    requestHeadersSpec.attributes(principal("zeny-client"));
                })
                .build();
    }

    @Override
    public User getUserByEmail(String email) {
        UserClientResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/users")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .body(UserClientResponse.class);

        return response.toModel();
    }
}
