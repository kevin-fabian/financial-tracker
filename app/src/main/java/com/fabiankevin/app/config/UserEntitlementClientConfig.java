package com.fabiankevin.app.config;

import com.fabiankevin.app.clients.DefaultUserEntitlementClient;
import com.fabiankevin.app.clients.UserEntitlementClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserEntitlementClientConfig {

    @Bean
    UserEntitlementClient userEntitlementClient(
            RestClient restClient,
            @Value("${app.clients.identity-service.base-url}") String baseUrl,
            @Value("${app.clients.identity-service.client-id}") String clientId) {
        return new DefaultUserEntitlementClient(restClient, baseUrl, clientId);
    }
}
