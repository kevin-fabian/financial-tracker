package com.fabiankevin.app.config;

import com.fabiankevin.app.clients.DefaultUserClient;
import com.fabiankevin.app.clients.UserClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserClientConfig {

    @Bean
    UserClient userClient(RestClient restClient) {
        return new DefaultUserClient(restClient,
                "http://localhost:9000/api",
                "financial-tracker-client");
    }
}
