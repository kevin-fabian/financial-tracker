package com.fabiankevin.app.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.databind.json.JsonMapper;

@RestClientTest(DefaultUserClient.class)
public class DefaultUserClientTest {
    @Autowired
    private DefaultUserClient userClient;

    private MockRestServiceServer mockServer;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
}
