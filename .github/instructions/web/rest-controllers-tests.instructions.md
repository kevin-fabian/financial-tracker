---
applyTo: '**/web/controllers/*Controller.java'
description: 'WebMvcTest controller implementation requirements.'
---

## WebMvcTest Controller implementation requirements

When writing @WebMvcTest implementation, please follow these guidelines:

- Place test classes in the `web/controllers` package.
- Annotate test classes with `@WebMvcTest(ControllerClass.class)` to configure Spring MVC and limit scanned beans to the target controller.
- Use MockMvc to perform HTTP requests and assert responses.
- Use ObjectMapper to serialize request to JSON string.


Examples:

```java
package com.example.web.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.times;
import com.example.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private UserService service;

    @Test
    void create_givenValidRequest_thenShouldCreateUser() throws Exception {
        when(service.create(any(CreateUserCommand.class)))
                .thenAnswer(invocation -> {
                    CreateUserCommand command = invocation.getArgument(0);
                    return User.builder()
                            .id(UUID.randomUUID())
                            .name(command.name())
                            .email(command.email())
                            .build();
                });

        mockMvc.perform(post("/v1/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                CreateUserRequest.builder()
                                        .name("John Doe")
                                        .email("john.doe@test.com")
                                        .build()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@test.com"));

        verify(service, times(1)).create(any(CreateUserCommand.class));
    }
}
```
