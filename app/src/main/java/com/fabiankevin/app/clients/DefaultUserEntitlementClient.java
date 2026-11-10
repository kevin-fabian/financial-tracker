package com.fabiankevin.app.clients;

import com.fabiankevin.app.clients.dtos.EntitlementClientResponse;
import com.fabiankevin.app.clients.dtos.RevokeEntitlementRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;
import static org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver.principal;

public class DefaultUserEntitlementClient implements UserEntitlementClient {
    private final RestClient restClient;

    public DefaultUserEntitlementClient(RestClient restClient, String baseUrl, String clientId) {
        this.restClient = restClient.mutate()
                .baseUrl(baseUrl)
                .defaultRequest(requestHeadersSpec -> {
                    requestHeadersSpec.attributes(clientRegistrationId(clientId));
                    requestHeadersSpec.attributes(principal(clientId));
                })
                .build();
    }

    @Override
    public EntitlementClientResponse grantPartnershipEntitlement(UUID userId) {
        try {
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/users/{userId}/grant-partnership").build(userId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatusCode.valueOf(401))) {
                throw new RuntimeException("Unauthorized when granting partnership entitlement for user: " + userId, e);
            }
            throw e;
        }
    }

    @Override
    public void revokeEntitlement(UUID userId, String entitlement) {
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/users/{userId}/revoke").build(userId))
                    .body(new RevokeEntitlementRequest(entitlement))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatusCode.valueOf(401))) {
                throw new RuntimeException("Unauthorized when revoking entitlement for user: " + userId, e);
            }
            throw e;
        }
    }
}
