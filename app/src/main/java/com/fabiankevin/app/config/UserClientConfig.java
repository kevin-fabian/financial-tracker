package com.fabiankevin.app.config;

import com.fabiankevin.app.clients.DefaultUserClient;
import com.fabiankevin.app.clients.UserClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserClientConfig {

    @Bean
    UserClient userClient(RestClient restClient,
                          @Value("${app.clients.identity-service.base-url}") String baseUrl,
                          @Value("${app.clients.identity-service.client-id}") String clientId) {
        return new DefaultUserClient(restClient, baseUrl, clientId);
    }
}
